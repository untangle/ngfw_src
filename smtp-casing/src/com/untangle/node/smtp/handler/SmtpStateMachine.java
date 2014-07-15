package com.untangle.node.smtp.handler;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.AUTHCommand;
import com.untangle.node.smtp.BeginMIMEToken;
import com.untangle.node.smtp.Command;
import com.untangle.node.smtp.CommandType;
import com.untangle.node.smtp.CommandWithEmailAddress;
import com.untangle.node.smtp.CompleteMIMEToken;
import com.untangle.node.smtp.ContinuedMIMEToken;
import com.untangle.node.smtp.MessageInfo;
import com.untangle.node.smtp.Response;
import com.untangle.node.smtp.SASLExchangeToken;
import com.untangle.node.smtp.SmtpTransaction;
import com.untangle.node.smtp.UnparsableCommand;
import com.untangle.node.smtp.handler.SmtpTransactionHandler.BlockOrPassResult;
import com.untangle.node.token.AbstractTokenHandler;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.PassThruToken;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenException;
import com.untangle.node.token.TokenResult;
import com.untangle.node.token.TokenResultBuilder;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.TCPNewSessionRequest;

public abstract class SmtpStateMachine extends AbstractTokenHandler
{
    private static final long LIKELY_TIMEOUT_LENGTH = 1000 * 60;// 1 minute

    private static final String SESSION_STATE_KEY = "SMTP-session-state";
    
    private static final String[] DEFAULT_ALLOWED_COMMANDS = { "DATA", "HELP", "HELO", "EHLO", "RCPT", "MAIL", "EXPN",
                                                               "QUIT", "RSET", "VRFY", "NOOP", "AUTH", "STARTTLS" };

    private static final String[] DEFAULT_ALLOWED_EXTENSIONS = { "DATA", "HELP", "HELO", "EHLO", "RCPT", "MAIL", "EXPN",
                                                                 "QUIT", "RSET", "VRFY", "NOOP", "SIZE", "DSN", "DELIVERBY",
                                                                 "AUTH", "AUTH=LOGIN", "OK", "STARTTLS" };

    private final Logger logger = Logger.getLogger(SmtpStateMachine.class);

    private SmtpTransactionHandler smtpTransactionHandler;

    public class SmtpSessionState
    {
        protected long clientTimestamp;
        protected long serverTimestamp;

        protected boolean isBufferAndTrickle;
        protected String heloName = null;

        // Time (absolute) when the class should stop being
        // nice, and nuke the connection with the client
        protected long quitAt;

        protected boolean shutingDownMode = false;

        protected boolean passthru = false;

        protected boolean clientTokensEnabled = true;
        protected List<Token> queuedClientTokens = new ArrayList<Token>();

        protected List<OutstandingRequest> outstandingRequests;
    }

    public SmtpStateMachine( )
    {
    }

    /**
     * Get the size over-which this class should no longer buffer. After this point, the Buffering is abandoned and the
     * <i>giveup-then-trickle</i> state is entered.
     */
    protected abstract int getGiveUpSz( NodeTCPSession session );

    /**
     * The maximum time (in relative milliseconds) that the client can wait for a response to DATA transmission.
     */
    protected abstract long getMaxClientWait( NodeTCPSession session );

    /**
     * The maximum time that the server can wait for a subsequent ("DATA") command.
     */
    protected abstract long getMaxServerWait( NodeTCPSession session );

    protected abstract boolean getScanningEnabled( NodeTCPSession session );
    

    /**
     * If true, this handler will continue to buffer even after trickling has begun (<i>buffer-and-trickle</i> mode).
     * The MIMEMessage can no longer be modified (i.e. it will not be passed downstream) but {@link #blockOrPass
     * blockOrPass()} will still be called once the complete message has been seen.
     */
    protected final boolean isBufferAndTrickle( NodeTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.isBufferAndTrickle;
    }

    /**
     * Determines, based on timestamps, if trickling should begin
     */
    protected boolean shouldBeginTrickle( NodeTCPSession session )
    {
        long maxWaitPeriod = Math.min( getMaxClientWait( session ), getMaxServerWait( session ) );
        long lastTimestamp = Math.min( getLastClientTimestamp( session ), getLastServerTimestamp( session ) );

        if ( maxWaitPeriod <= 0 ) {
            // Time equal-to or below zero means give-up
            return false;
        }

        maxWaitPeriod = (long) (maxWaitPeriod * 0.95);// TODO bscott a real "slop" factor - not a guess

        return (System.currentTimeMillis() - lastTimestamp) < maxWaitPeriod ? false : true;
    }

    private final void updateTimestamps( SmtpSessionState state, boolean client, boolean server )
    {
        long now = System.currentTimeMillis();
        if (client) {
            state.clientTimestamp = now;
        }
        if (server) {
            state.serverTimestamp = now;
        }
    }

    public void startShutingDown( NodeTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        state.shutingDownMode = true;
    }

    public boolean isShutingDown( NodeTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.shutingDownMode;
    }

    public String getHeloName( NodeTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.heloName;
    }
    
    
    @Override
    public void handleNewSessionRequest( TCPNewSessionRequest tsr )
    {
        SmtpSessionState state = new SmtpSessionState();

        state.isBufferAndTrickle = false;

        // Build-out request queue
        updateTimestamps( state, true, true);
        state.outstandingRequests = new LinkedList<OutstandingRequest>();

        // The first message passed for SMTP is actualy from the server.
        // Place a Response handler into the OutstandingRequest queue to handle this and call our SessionHandler with
        // the initial salutation
        state.outstandingRequests.add(new OutstandingRequest(new ResponseCompletion()
        {
            @Override
            public void handleResponse( NodeTCPSession session, Response resp, TokenResultBuilder ts)
            {
                handleOpeningResponse( session, resp, ts);
            }
        }));

        tsr.attach( SESSION_STATE_KEY, state );
    }
    
    @Override
    public TokenResult handleClientToken( NodeTCPSession session, Token token ) throws TokenException
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        updateTimestamps( state, true, false );

        List<Token> queuedClientTokens = state.queuedClientTokens;
        TokenResultBuilder trb = new TokenResultBuilder();

        // First add the token, to preserve ordering if we have a queue (and while draining someone changes the
        // enablement flag)
        queuedClientTokens.add(token);

        if (!state.clientTokensEnabled) {
            logger.debug("[handleClientToken] Queuing Token \"" + token.getClass().getName() + "\" (" + queuedClientTokens.size() + " tokens queued)");
        } else {
            // Important - the enablement of client tokens could change as this loop is running.
            while (queuedClientTokens.size() > 0 && state.clientTokensEnabled) {
                if (queuedClientTokens.size() > 1) {
                    logger.debug("[handleClientToken] Draining Queued Token \""
                            + queuedClientTokens.get(0).getClass().getName() + "\" (" + queuedClientTokens.size()
                            + " tokens remain)");
                }
                handleClientTokenImpl( session, queuedClientTokens.remove(0), trb );
            }
        }
        if (queuedClientTokens.size() > 0) {
            logger.debug("[handleClientToken] returning with (" + queuedClientTokens.size() + " queued tokens)");
        }
        updateTimestamps( state, trb.hasDataForClient(), trb.hasDataForServer() );
        return trb.getTokenResult();
    }

    @Override
    public TokenResult handleServerToken( NodeTCPSession session, Token token ) throws TokenException
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        updateTimestamps( state, false, true );

        TokenResultBuilder trb = new TokenResultBuilder();
        List<Token> queuedClientTokens = state.queuedClientTokens;

        while (queuedClientTokens.size() > 0 && state.clientTokensEnabled) {
            logger.debug("[handleServerToken] Draining Queued Client Token \""
                    + queuedClientTokens.get(0).getClass().getName() + "\" (" + queuedClientTokens.size()
                    + " tokens remain)");
            handleClientTokenImpl( session, queuedClientTokens.remove(0), trb );
        }
        handleServerTokenImpl( session, token, trb );

        // Important - the enablement of client tokens could change as this loop is running.
        while (queuedClientTokens.size() > 0 && state.clientTokensEnabled) {
            logger.debug("[handleServerToken] Draining Queued Token \""
                    + queuedClientTokens.get(0).getClass().getName() + "\" (" + queuedClientTokens.size()
                    + " tokens remain)");
            handleClientTokenImpl( session, queuedClientTokens.remove(0), trb );
        }
        if (queuedClientTokens.size() > 0) {
            logger.debug("[handleServerToken] returning with (" + queuedClientTokens.size() + " queued tokens)");
        }
        updateTimestamps( state, trb.hasDataForClient(), trb.hasDataForServer() );
        return trb.getTokenResult();
    }

    // FROM Client
    private final void handleClientTokenImpl( NodeTCPSession session, Token token, TokenResultBuilder trb ) throws TokenException
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );

        // Check for passthrough
        if ( state.passthru || !getScanningEnabled( session ) ) {
            logger.debug("(In passthru, client token) passing token of type " + token.getClass().getName());
            trb.addTokenForServer(token);
            return;
        }
        if (token instanceof SASLExchangeToken || token instanceof AUTHCommand || token instanceof PassThruToken) {
            logger.debug("Received " + token.getClass().getName() + " token");
            if (token instanceof PassThruToken) {
                state.passthru = true;
                logger.debug("(client token) Entering Passthru");
            }
            trb.addTokenForServer(token);
            return;
        }
        if (token instanceof CommandWithEmailAddress && !state.shutingDownMode) {
            smtpTransactionHandler = getOrCreateTxHandler();
            if (((CommandWithEmailAddress) token).getType() == CommandType.MAIL)
                smtpTransactionHandler.handleMAILCommand( session, (CommandWithEmailAddress) token, this, trb);
            else
                smtpTransactionHandler.handleRCPTCommand( session, (CommandWithEmailAddress) token, this, trb);
            return;
        }
        if (token instanceof Command) {
            handleCommand( session, trb, (Command) token );
            return;
        }
        if (token instanceof Chunk) {
            trb.addTokenForServer((Chunk) token);
            return;
        }
        // the rest of the commands are handled differently if in shutdown mode
        if (state.shutingDownMode) {
            handleCommandInShutDown( session, token, trb );
            return;
        }
        if (token instanceof BeginMIMEToken) {
            handleBeginMIME( session, trb, (BeginMIMEToken) token);
            return;
        }
        if (token instanceof ContinuedMIMEToken) {
            handleContinuedMIME( session, trb, (ContinuedMIMEToken) token);
            return;
        }
        if (token instanceof CompleteMIMEToken) {
            handleCompleteMIME( session, trb, (CompleteMIMEToken) token);
            return;
        }
        logger.error("(client token) Unexpected Token of type \"" + token.getClass().getName() + "\".  Pass it along");
        trb.addTokenForServer(token);
    }

    // FROM Server
    private final void handleServerTokenImpl( NodeTCPSession session, Token token, TokenResultBuilder trb ) throws TokenException
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        if ( state.passthru || !getScanningEnabled( session ) ) {
            logger.debug("(In passthru, server token) passing token of type " + token.getClass().getName());
            trb.addTokenForClient(token);
            return;
        }
        if (token instanceof SASLExchangeToken) {
            logger.debug("Received SASL token");
            trb.addTokenForClient(token);
            return;
        }
        // Passthru
        if (token instanceof PassThruToken) {
            logger.debug("(server token) Entering Passthru");
            state.passthru = true;
            trb.addTokenForClient(token);
            return;
        } else if (token instanceof Response) {
            handleResponse( session, trb, (Response) token);
            return;
        } else if (token instanceof Chunk) {
            trb.addTokenForClient((Chunk) token);
            return;
        }
        logger.error("Unexpected Token of type \"" + token.getClass().getName() + "\".  Pass it along");
        trb.addTokenForClient(token);
    }

    /**
     * Edge-case handler. When an SMTP Session is created, the server is the first actor to send data. However, the
     * SessionHandler did not "see" any request corresponding to this response and could not have installed a
     * ResponseCompletion. Instead, this method is used to handle this "misaligned" response.
     * 
     * This built-in method may be overriden.
     * 
     * @param resp
     *            the response
     * @param actions
     *            the available actions.
     */
    public void handleOpeningResponse( NodeTCPSession session, Response resp, TokenResultBuilder ts )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        if ( state.shutingDownMode ) {
            if ( timedOut( session ) ) {
                return;
            } else {
                send421(ts);
            }
        } else {
            sendResponseToClient(resp, ts);
        }
    }

    /**
     * returns true if the SMTP extension (advertisement) is allowed
     * can be overridden to customize supported extensions
     */
    protected boolean isAllowedExtension( String extension )
    {
        // Thread safety
        String str = extension.toUpperCase();
        for ( String permitted : DEFAULT_ALLOWED_EXTENSIONS ) {
            if ( permitted.equals( str ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * returns true if the SMTP command is allowed
     * can be overridden to customize supported commands
     */
    protected boolean isAllowedCommand( String command )
    {
        // Thread safety
        String str = command.toUpperCase();
        for ( String permitted : DEFAULT_ALLOWED_COMMANDS ) {
            if ( permitted.equals( str ) ) {
                return true;
            }
        }
        return false;
    }

    /********************************************************************************************/

    /**
     * Process any Synthetic actions
     */
    private void processSynths(List<Response> synths, TokenResultBuilder trb)
    {
        if (synths == null) {
            return;
        }
        while (synths.size() > 0) {
            Response action = synths.remove(0);
            sendResponseToClient(action, trb);
        }
    }

    private void followup(List<Response> immediateActions, TokenResultBuilder trb)
    {
        if (immediateActions != null && immediateActions.size() > 0) {
            processSynths(immediateActions, trb);
        }
    }

    /**
     * Method which removes any unknown ESMTP extensions
     */
    private Response fixupEHLOResponse(Response resp)
    {

        String[] respLines = resp.getArgs();
        if (respLines == null || respLines.length < 2) {
            // Note first line is the "doman" junk
            return resp;
        }

        List<String> finalList = new ArrayList<String>();
        finalList.add(respLines[0]);// Add domain line

        for (int i = 1; i < respLines.length; i++) {
            String verb = getCapabilitiesLineVerb(respLines[i]);
            if ( isAllowedExtension( verb ) ) {
                logger.debug("Allowing ESMTP response line \"" + respLines[i] + "\" to go to client");
                finalList.add(respLines[i]);
            } else {
                logger.debug("Removing unknown extension \"" + respLines[i] + "\" (" + verb + ")");
            }
        }

        String[] newRespLines = finalList.toArray(new String[finalList.size()]);
        return new Response(resp.getCode(), newRespLines);
    }

    /**
     * Picks the verb out-of an ESMTP response line (i.e. "SIZE" out of "SIZE 13546654").
     */
    private String getCapabilitiesLineVerb(String str)
    {
        str = str.trim();
        int index = str.indexOf(' ');
        if (index < 0) {
            return str;
        }
        return str.substring(0, index);
    }

    private void handleCommand( NodeTCPSession session, TokenResultBuilder resultBuilder, Command cmd )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );

        logger.debug("Received Command \"" + cmd.getCmdString() + "\"" + " of type \"" + cmd.getClass().getName() + "\"");
        List<Response> immediateActions = new LinkedList<Response>();

        // Check for allowed commands
        if ((!(cmd instanceof UnparsableCommand)) && !isAllowedCommand( cmd.getCmdString() )) {
            logger.warn("Enqueuing negative response to " + "non-allowed command \"" + cmd.getCmdString() + "\"" + " (" + cmd.getArgString() + ")");
            appendSyntheticResponse( session, new Response(500, "Syntax error, command unrecognized"), immediateActions);
            if (immediateActions.size() > 0) {
                processSynths(immediateActions, resultBuilder);
            }
            return;
        }

        // Check for "EHLO" and "HELO"
        if (cmd.getType() == CommandType.EHLO) {
            logger.debug("Enqueuing private response handler to EHLO command so unknown extensions can be disabled");
            sendCommandToServer( session, cmd, new ResponseCompletion() {
                    @Override
                    public void handleResponse( NodeTCPSession session, Response resp, TokenResultBuilder ts)
                    {
                        logger.debug("Processing response to EHLO Command");
                        sendResponseToClient(fixupEHLOResponse(resp), ts);
                    }
                }, resultBuilder);
            state.heloName = cmd.getArgString();
            followup(immediateActions, resultBuilder);
            return;
        } else if (cmd.getType() == CommandType.HELO) {
            state.heloName = cmd.getArgString();
            // fall through and continue like before
        }

        if ( state.shutingDownMode ) {
            transactionEnded(smtpTransactionHandler);
            handleCommandInShutDown( session, cmd, resultBuilder );
            return;
        }
        if (smtpTransactionHandler != null) {
            if (cmd.getType() == CommandType.RSET) {
                smtpTransactionHandler.handleRSETCommand( session, cmd, this, resultBuilder );
                smtpTransactionHandler = null;
            } else {
                smtpTransactionHandler.handleCommand( session, cmd, this, resultBuilder, immediateActions );
            }
        } else {
            // Odd case
            if (cmd.getType() == CommandType.DATA) {
                smtpTransactionHandler = getOrCreateTxHandler();
                smtpTransactionHandler.handleCommand( session, cmd, this, resultBuilder, immediateActions );
            } else {

                logger.debug("[handleCommand] with command of type \"" + cmd.getType() + "\"");
                sendCommandToServer( session, cmd, SmtpTransactionHandler.PASSTHRU_RESPONSE_COMPLETION, resultBuilder);
            }
        }
        followup(immediateActions, resultBuilder);
    }

    private void handleCommandInShutDown( NodeTCPSession session, Token token, TokenResultBuilder ts )
    {
        // Check for our timeout
        if ( timedOut( session ) ) {
            return;
        }
        if (token instanceof CommandWithEmailAddress || token instanceof Command) {
            Command command = (Command) token;
            // Check for "special" commands
            if (command.getType() == CommandType.QUIT) {
                sendFINToClient( session );
                return;
            }
            if (command.getType() == CommandType.RSET) {
                ts.addTokenForClient(new Response(250, "OK"));
                return;
            }
            if (command.getType() == CommandType.NOOP) {
                ts.addTokenForClient(new Response(250, "OK"));
                return;
            }
        }
        send421(ts);
    }

    private boolean timedOut( NodeTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        if (System.currentTimeMillis() > state.quitAt) {
            sendFINToClient( session );
            return true;
        }
        return false;
    }

    private SmtpTransactionHandler getOrCreateTxHandler()
    {
        if (smtpTransactionHandler == null) {
            logger.debug("Creating new Transaction Handler");
            smtpTransactionHandler = new SmtpTransactionHandler(new SmtpTransaction());
        }
        return smtpTransactionHandler;
    }

    private void handleBeginMIME( NodeTCPSession session, TokenResultBuilder resultBuilder, BeginMIMEToken token )
    {
        List<Response> immediateActions = new LinkedList<Response>();
        smtpTransactionHandler = getOrCreateTxHandler();
        smtpTransactionHandler.handleBeginMIME( session, token, this, immediateActions, resultBuilder );
        followup(immediateActions, resultBuilder);
    }

    private void handleContinuedMIME( NodeTCPSession session, TokenResultBuilder resultBuilder, ContinuedMIMEToken token)
    {
        List<Response> immediateActions = new LinkedList<Response>();
        SmtpTransactionHandler transactionHandler = getOrCreateTxHandler();
        if (token.isLast()) {
            smtpTransactionHandler = null;
        }
        transactionHandler.handleContinuedMIME( session, token, this, immediateActions, resultBuilder );
        followup(immediateActions, resultBuilder);
    }

    private void handleResponse( NodeTCPSession session, TokenResultBuilder resultBuilder, Response resp)
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        
        logger.debug("[handleResponse()] with code " + resp.getCode());
        if (state.outstandingRequests.size() == 0) {
            long timeDiff = System.currentTimeMillis() - getLastServerTimestamp( session );
            if (timeDiff > LIKELY_TIMEOUT_LENGTH) {
                logger.warn("Unsolicited response from server.  Likely a timeout (" + timeDiff
                        + " millis since last communication)");
            } else {
                logger.info("Response received without a registered handler");
            }
            resultBuilder.addTokenForClient(resp);
            return;
        }
        OutstandingRequest or = state.outstandingRequests.remove(0);
        or.getResponseCompletion().handleResponse( session, resp, resultBuilder );
        processSynths(or.getAdditionalActions(), resultBuilder);
    }

    private void handleCompleteMIME( NodeTCPSession session, TokenResultBuilder resultBuilder, CompleteMIMEToken token)
    {

        SmtpTransactionHandler transactionHandler = getOrCreateTxHandler();

        // Looks odd, but the Transaction is complete so just assign the current handler to null.
        smtpTransactionHandler = null;
        List<Response> immediateActions = new LinkedList<Response>();
        transactionHandler.handleCompleteMIME( session, token, this, immediateActions, resultBuilder );
        followup(immediateActions, resultBuilder);
    }

    @Override
    public final void handleClientFin( NodeTCPSession session ) throws TokenException
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        state.outstandingRequests.add(new OutstandingRequest(SmtpTransactionHandler.NOOP_RESPONSE_COMPLETION));
        logger.debug("Passing along client FIN");
        session.shutdownServer();
    }

    @Override
    public final void handleServerFin( NodeTCPSession session ) throws TokenException
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        if (!state.shutingDownMode) {
            logger.debug("Passing along server FIN");
            session.shutdownClient();
        } else {
            logger.debug("Supress server FIN");
        }
    }

    @Override
    public void handleFinalized( NodeTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        if (smtpTransactionHandler != null && !state.shutingDownMode) {
            smtpTransactionHandler.handleFinalized();
        }
        session.cleanupTempFiles();
    }

    public void sendResponseToClient(Response resp, TokenResultBuilder ts)
    {
        logger.debug("Sending response " + resp.getCode() + " to client");
        ts.addTokenForClient(resp);
    }

    public void transactionEnded(SmtpTransactionHandler handler)
    {
        if (smtpTransactionHandler == handler) {
            logger.debug("Deregistering transaction handler");
            smtpTransactionHandler = null;
        }
    }

    public void sendFINToServer( NodeTCPSession session, ResponseCompletion compl )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        logger.debug("Sending FIN to server");
        state.outstandingRequests.add(new OutstandingRequest(compl));
        session.shutdownServer();
    }

    public void sendFINToClient( NodeTCPSession session )
    {
        logger.debug("Sending FIN to client");
        session.shutdownClient();
    }

    public void sendCommandToServer( NodeTCPSession session, Command command, ResponseCompletion compl, TokenResultBuilder ts )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        logger.debug("Sending Command " + command.getType() + " to server");
        ts.addTokenForServer(command);
        state.outstandingRequests.add(new OutstandingRequest(compl));
    }

    public void sendBeginMIMEToServer( NodeTCPSession session, BeginMIMEToken token, TokenResultBuilder ts )
    {
        logger.debug("Sending BeginMIMEToken to server");
        ts.addTokenForServer(token);
    }

    public void sendContinuedMIMEToServer( NodeTCPSession session, ContinuedMIMEToken token, TokenResultBuilder ts )
    {
        logger.debug("Sending intermediate ContinuedMIMEToken to server");
        ts.addTokenForServer(token);
    }

    public void sendFinalMIMEToServer( NodeTCPSession session, ContinuedMIMEToken token, ResponseCompletion compl, TokenResultBuilder ts )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        logger.debug("Sending final ContinuedMIMEToken to server");
        ts.addTokenForServer(token);
        state.outstandingRequests.add(new OutstandingRequest(compl));
    }

    public void sentWholeMIMEToServer( NodeTCPSession session, CompleteMIMEToken token, ResponseCompletion compl, TokenResultBuilder ts )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        logger.debug("Sending whole MIME to server");
        ts.addTokenForServer(token);
        state.outstandingRequests.add(new OutstandingRequest(compl));
    }

    /**
     * Appends a response either to the list of outstanding responses, if there are outstanding responses, or to the
     * list of immediate actions, if there are no outstanding responses.
     * 
     * @param synth
     * @param immediateActions
     * 
     */
    public void appendSyntheticResponse( NodeTCPSession session, Response synth, List<Response> immediateActions )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        logger.debug("Appending synthetic response");
        if (state.outstandingRequests.size() == 0) {
            immediateActions.add(synth);
        } else {
            state.outstandingRequests.get(state.outstandingRequests.size() - 1).getAdditionalActions().add(synth);
        }
    }

    /**
     * Get the absolute time (based on the local clock) of when the client last sent or was-sent a unit of data.
     */
    public long getLastClientTimestamp( NodeTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.clientTimestamp;
    }

    /**
     * Get the absolute time (based on the local clock) of when the server last was sent or sent a unit of data.
     */
    public long getLastServerTimestamp( NodeTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.serverTimestamp;
    }

    /**
     * Get the client IP address
     */
    public InetAddress getClientAddress( NodeTCPSession session )
    {
        return session.getClientAddr();
    }

    /**
     * Re-enable the flow of Client tokens. If this method is called while Client Tokens are not
     * {@link #disableClientTokens disabled}, this has no effect.
     */
    protected void enableClientTokens( NodeTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        if (!state.clientTokensEnabled) {
            logger.debug("Re-enabling Client Tokens");
            state.clientTokensEnabled = true;
        } else {
            logger.debug("Redundant call to enable Client Tokens");
        }
    }

    /**
     * Disable the flow of client tokens. No more calls to the Handler will be made with Client Tokens until the
     * {@link #enableClientTokens enable method} is called.
     */
    protected void disableClientTokens( NodeTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        if (state.clientTokensEnabled) {
            logger.debug("Disabling Client Tokens");
            state.clientTokensEnabled = false;
        } else {
            logger.debug("Redundant call to disable Client Tokens");
        }
    }

    private void send421(TokenResultBuilder ts)
    {
        ts.addTokenForClient(new Response(421, "Service not available, closing transmission channel"));
    }

    public abstract ScannedMessageResult blockPassOrModify( NodeTCPSession session, MimeMessage m_msg, SmtpTransaction transaction, MessageInfo messageInfo );

    public abstract BlockOrPassResult blockOrPass( NodeTCPSession session, MimeMessage m_msg, SmtpTransaction transaction, MessageInfo messageInfo );
}
