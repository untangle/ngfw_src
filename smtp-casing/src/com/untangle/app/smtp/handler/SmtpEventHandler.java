/**
 * $Id$
 */
package com.untangle.app.smtp.handler;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.untangle.app.smtp.AUTHCommand;
import com.untangle.app.smtp.BeginMIMEToken;
import com.untangle.app.smtp.Command;
import com.untangle.app.smtp.CommandType;
import com.untangle.app.smtp.CommandWithEmailAddress;
import com.untangle.app.smtp.CompleteMIMEToken;
import com.untangle.app.smtp.ContinuedMIMEToken;
import com.untangle.app.smtp.SmtpMessageEvent;
import com.untangle.app.smtp.Response;
import com.untangle.app.smtp.SASLExchangeToken;
import com.untangle.app.smtp.SmtpTransaction;
import com.untangle.app.smtp.UnparsableCommand;
import com.untangle.app.smtp.PassThruToken;
import com.untangle.app.smtp.handler.SmtpTransactionHandler.BlockOrPassResult;
import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.ReleaseToken;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.AbstractEventHandler;

/**
 * Handle SMTP server event.
 */
public abstract class SmtpEventHandler extends AbstractEventHandler
{
    private static final long LIKELY_TIMEOUT_LENGTH = 1000 * 60;// 1 minute

    private static final String SESSION_STATE_KEY = "SMTP-session-state";
    
    private static final String[] DEFAULT_ALLOWED_COMMANDS = { "DATA", "HELP", "HELO", "EHLO", "RCPT", "MAIL", "EXPN",
                                                               "QUIT", "RSET", "VRFY", "NOOP", "AUTH", "STARTTLS" };

    private static final String[] DEFAULT_ALLOWED_EXTENSIONS = { "DATA", "HELP", "HELO", "EHLO", "RCPT", "MAIL", "EXPN",
                                                                 "QUIT", "RSET", "VRFY", "NOOP", "SIZE", "DSN", "DELIVERBY",
                                                                 "AUTH", "AUTH=LOGIN", "OK", "STARTTLS" };

    private final Logger logger = Logger.getLogger(SmtpEventHandler.class);

    /**
     * SMTP session state.
     */
    public class SmtpSessionState
    {
        protected long timestamp;

        protected boolean isBufferAndTrickle;

        protected String heloName = null;

        // Time (absolute) when the class should stop being
        // nice, and nuke the connection with the client
        protected long quitAt;

        protected boolean shutingDownMode = false;

        protected boolean passthru = false;

        protected boolean clientTokensEnabled = true;

        protected List<Token> queuedClientTokens = new ArrayList<>();

        protected List<OutstandingRequest> outstandingRequests;

        protected SmtpTransactionHandler smtpTransactionHandler;
    }

    /**
     * Initialize instance of SmtpEventHandler.
     * @return instance of SmtpEventHandler.
     */
    public SmtpEventHandler( )
    {
    }

    /**
     * Get the size over-which this class should no longer buffer. After this point, the Buffering is abandoned and the
     * <i>giveup-then-trickle</i> state is entered.
     * @param session AppTCPSession to handle.
     * @return long of size that server gave up.
     */
    protected abstract int getGiveUpSz( AppTCPSession session );

    /**
     * The maximum time (in relative milliseconds) that the client can wait for a response to DATA transmission.
     * @param session AppTCPSession to handle.
     * @return long of maximum client wait.
     */
    protected abstract long getMaxClientWait( AppTCPSession session );

    /**
     * The maximum time that the server can wait for a subsequent ("DATA") command.
     * @param session AppTCPSession to handle.
     * @return long of max server wait.
     */
    protected abstract long getMaxServerWait( AppTCPSession session );

    /**
     * Return true if scanning is enabled for this session
     * @param session AppTCPSession to handle.
     * @return if true, scanning enabled for session, otherwise false.
     */
    protected abstract boolean getScanningEnabled( AppTCPSession session );

    /**
     * If true, this handler will continue to buffer even after trickling has begun (<i>buffer-and-trickle</i> mode).
     * The MIMEMessage can no longer be modified (i.e. it will not be passed downstream) but {@link #blockOrPass
     * blockOrPass()} will still be called once the complete message has been seen.
     * @param session AppTCPSession to handle.
     * @return true if buffer and trickle enabled, false otherwise.
     */
    protected final boolean isBufferAndTrickle( AppTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.isBufferAndTrickle;
    }

    /**
     * Determines, based on timestamps, if trickling should begin
     * @param session AppTCPSession to handle.
     * @return true if remote should beging trickle, false otherwise.
     */
    protected boolean shouldBeginTrickle( AppTCPSession session )
    {
        long maxWaitPeriod = Math.min( getMaxClientWait( session ), getMaxServerWait( session ) );
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        long lastTimestamp = state.timestamp;

        if ( maxWaitPeriod <= 0 ) {
            // Time equal-to or below zero means give-up
            return false;
        }

        maxWaitPeriod = (long) (maxWaitPeriod * 0.95);// TODO bscott a real "slop" factor - not a guess

        return (System.currentTimeMillis() - lastTimestamp) < maxWaitPeriod ? false : true;
    }

    /**
     * Update state with current timestamp.
     * @param state SmtpSessionState to update.
     */
    private final void updateTimestamps( SmtpSessionState state )
    {
        state.timestamp = System.currentTimeMillis();
    }

    /**
     * Indicate shutdown to begin.
     * @param session AppTCPSession to handle.
     */
    public void startShutingDown( AppTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        state.shutingDownMode = true;
    }

    /**
     * Determine if shutting down.
     * @param session AppTCPSession to handle.
     * @return if true, shutting down, otherwise false.
     */
    public boolean isShutingDown( AppTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.shutingDownMode;
    }

    /**
     * Return HELO value.
     * @param session AppTCPSession to handle.
     * @return        String of HELO value.
     */
    public String getHeloName( AppTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.heloName;
    }

    /**
     * Handle TCP new session request.
     * @param tsr TCPNewSessionRequest to handle.
     */
    @Override
    public void handleTCPNewSessionRequest( TCPNewSessionRequest tsr )
    {
        SmtpSessionState state = new SmtpSessionState();

        state.isBufferAndTrickle = false;

        // Build-out request queue
        updateTimestamps( state );
        state.outstandingRequests = new LinkedList<>();

        // The first message passed for SMTP is actualy from the server.
        // Place a Response handler into the OutstandingRequest queue to handle this and call our SessionHandler with
        // the initial salutation
        state.outstandingRequests.add(new OutstandingRequest(new ResponseCompletion()
        {
            /**
             * Handle response.
             * @param session AppTCPSession to handle.
             * @param resp    Response to process.
             */
            @Override
            public void handleResponse( AppTCPSession session, Response resp )
            {
                handleOpeningResponse( session, resp );
            }
        }));

        tsr.attach( SESSION_STATE_KEY, state );
    }

    /**
     * Handle TCP client object.
     * @param session AppTCPSession to handle.
     * @param obj     Object to process.
     */
    @Override
    public final void handleTCPClientObject( AppTCPSession session, Object obj )
    {
        Token token = (Token) obj;

        logger.debug("Received from client: " + obj);

        if (token instanceof ReleaseToken) {
            handleTCPFinalized( session );
            session.sendObjectToServer( token );
            session.release();
            return;
        }

        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        updateTimestamps( state );

        List<Token> queuedClientTokens = state.queuedClientTokens;

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
                handleClientTokenImpl( session, queuedClientTokens.remove(0) );
            }
        }
        if (queuedClientTokens.size() > 0) {
            logger.debug("[handleClientToken] returning with (" + queuedClientTokens.size() + " queued tokens)");
        }
        updateTimestamps( state );
    }

    /**
     * Handle TCP server object.
     * @param session AppTCPSession to handle.
     * @param obj     Object to process.
     */
    @Override
    public final void handleTCPServerObject( AppTCPSession session, Object obj )
    {
        Token token = (Token) obj;

        logger.debug("Received from server: " + obj);
        
        if (token instanceof ReleaseToken) {
            handleTCPFinalized( session );
            session.sendObjectToClient( token );
            session.release();
            return;
        }

        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        updateTimestamps( state );

        List<Token> queuedClientTokens = state.queuedClientTokens;

        while (queuedClientTokens.size() > 0 && state.clientTokensEnabled) {
            logger.debug("[handleServerToken] Draining Queued Client Token \""
                    + queuedClientTokens.get(0).getClass().getName() + "\" (" + queuedClientTokens.size()
                    + " tokens remain)");
            handleClientTokenImpl( session, queuedClientTokens.remove(0) );
        }
        handleServerTokenImpl( session, token );

        // Important - the enablement of client tokens could change as this loop is running.
        while (queuedClientTokens.size() > 0 && state.clientTokensEnabled) {
            logger.debug("[handleServerToken] Draining Queued Token \""
                    + queuedClientTokens.get(0).getClass().getName() + "\" (" + queuedClientTokens.size()
                    + " tokens remain)");
            handleClientTokenImpl( session, queuedClientTokens.remove(0) );
        }
        if (queuedClientTokens.size() > 0) {
            logger.debug("[handleServerToken] returning with (" + queuedClientTokens.size() + " queued tokens)");
        }
        updateTimestamps( state );

        return;
    }

    /**
     * Handle from client.
     * @param session AppTCPSession to handle.
     * @param token     Token to process.
     */
    private final void handleClientTokenImpl( AppTCPSession session, Token token )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );

        // Check for passthrough
        if ( state.passthru || !getScanningEnabled( session ) ) {
            logger.debug("(In passthru, client token) passing token of type " + token.getClass().getName());
            session.sendObjectToServer( token );
            return;
        }
        if (token instanceof SASLExchangeToken || token instanceof AUTHCommand || token instanceof PassThruToken) {
            logger.debug("Received " + token.getClass().getName() + " token");
            if (token instanceof PassThruToken) {
                state.passthru = true;
                logger.debug("(client token) Entering Passthru");
            }
            session.sendObjectToServer( token );
            return;
        }
        if (token instanceof CommandWithEmailAddress && !state.shutingDownMode) {
            state.smtpTransactionHandler = getOrCreateTxHandler( state );
            if (((CommandWithEmailAddress) token).getType() == CommandType.MAIL)
                state.smtpTransactionHandler.handleMAILCommand( session, (CommandWithEmailAddress) token, this );
            else
                state.smtpTransactionHandler.handleRCPTCommand( session, (CommandWithEmailAddress) token, this );
            return;
        }
        if (token instanceof Command) {
            handleCommand( session, (Command) token );
            return;
        }
        if (token instanceof ChunkToken) {
            session.sendObjectToServer( token );
            return;
        }
        // the rest of the commands are handled differently if in shutdown mode
        if (state.shutingDownMode) {
            handleCommandInShutDown( session, token );
            return;
        }
        if (token instanceof BeginMIMEToken) {
            handleBeginMIME( session, (BeginMIMEToken) token);
            return;
        }
        if (token instanceof ContinuedMIMEToken) {
            handleContinuedMIME( session, (ContinuedMIMEToken) token);
            return;
        }
        if (token instanceof CompleteMIMEToken) {
            handleCompleteMIME( session, (CompleteMIMEToken) token);
            return;
        }
        logger.error("(client token) Unexpected Token of type \"" + token.getClass().getName() + "\".  Pass it along");
        session.sendObjectToServer( token );
    }

    /**
     * Handle from server.
     * @param session AppTCPSession to handle.
     * @param token     Token to process.
     */
    private final void handleServerTokenImpl( AppTCPSession session, Token token )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        if ( state.passthru || !getScanningEnabled( session ) ) {
            logger.debug("(In passthru, server token) passing token of type " + token.getClass().getName());
            session.sendObjectToClient(token);
            return;
        }
        if (token instanceof SASLExchangeToken) {
            logger.debug("Received SASL token");
            session.sendObjectToClient(token);
            return;
        }
        // Passthru
        if (token instanceof PassThruToken) {
            logger.debug("(server token) Entering Passthru");
            state.passthru = true;
            session.sendObjectToClient(token);
            return;
        } else if (token instanceof Response) {
            handleResponse( session, (Response) token);
            return;
        } else if (token instanceof ChunkToken) {
            session.sendObjectToClient((ChunkToken) token);
            return;
        }
        logger.error("Unexpected Token of type \"" + token.getClass().getName() + "\".  Pass it along");
        session.sendObjectToClient(token);
    }

    /**
     * Edge-case handler. When an SMTP Session is created, the server is the first actor to send data. However, the
     * SessionHandler did not "see" any request corresponding to this response and could not have installed a
     * ResponseCompletion. Instead, this method is used to handle this "misaligned" response.
     * 
     * This built-in method may be overriden.
     * 
     * @param session AppTCPSession to process.
     * @param resp Respone to process.
     */
    public void handleOpeningResponse( AppTCPSession session, Response resp )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        if ( state.shutingDownMode ) {
            if ( timedOut( session ) ) {
                return;
            } else {
                send421( session );
            }
        } else {
            sendResponseToClient( session, resp );
        }
    }

    /**
     * returns true if the SMTP extension (advertisement) is allowed
     * can be overridden to customize supported extensions
     * @param extension String of SMTP extension to process.
     * @param session AppTCPSession to process.
     * @return true if SMTP extension is allowed, false otherwise.
     */
    protected boolean isAllowedExtension( String extension, AppTCPSession session )
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
     * @param command String of SMTP extension to process.
     * @param session AppTCPSession to process.
     * @return true if SMTP command is allowed, false otherwise.
     */
    protected boolean isAllowedCommand( String command, AppTCPSession session )
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
     * @param session AppTCPSession to process.
     * @param synths List of Response toprocess.
     */
    private void processSynths( AppTCPSession session, List<Response> synths )
    {
        if (synths == null) {
            return;
        }
        while (synths.size() > 0) {
            Response action = synths.remove(0);
            sendResponseToClient( session, action );
        }
    }

    /**
     * Process any immediate actions.
     * @param session AppTCPSession to process.
     * @param immediateActions List of Response toprocess.
     */
    private void followup( AppTCPSession session, List<Response> immediateActions )
    {
        if (immediateActions != null && immediateActions.size() > 0) {
            processSynths( session, immediateActions );
        }
    }

    /**
     * Method which removes any unknown ESMTP extensions
     * @param resp Response to process.
     * @param session AppTCPSession to process.
     * @return Response indicating removal.
     */
    private Response fixupEHLOResponse(Response resp, AppTCPSession session)
    {

        String[] respLines = resp.getArgs();
        if (respLines == null || respLines.length < 2) {
            // Note first line is the "doman" junk
            return resp;
        }

        List<String> finalList = new ArrayList<>();
        finalList.add(respLines[0]);// Add domain line

        for (int i = 1; i < respLines.length; i++) {
            String verb = getCapabilitiesLineVerb(respLines[i]);
            if ( isAllowedExtension( verb, session ) ) {
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
     * @param str String to find.
     * @return String of capability.
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

    /**
     * Process SMTP command.
     * @param session AppTCPSession to process.
     * @param cmd     Command to process.
     */
    private void handleCommand( AppTCPSession session, Command cmd )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );

        logger.debug("Received Command \"" + cmd.getCmdString() + "\"" + " of type \"" + cmd.getClass().getName() + "\"");
        List<Response> immediateActions = new LinkedList<>();

        // Check for allowed commands
        if ((!(cmd instanceof UnparsableCommand)) && !isAllowedCommand( cmd.getCmdString(), session )) {
            logger.warn("Enqueuing negative response to " + "non-allowed command \"" + cmd.getCmdString() + "\"" + " (" + cmd.getArgString() + ")");
            appendSyntheticResponse( session, new Response(500, "Syntax error, command unrecognized"), immediateActions);
            if (immediateActions.size() > 0) {
                processSynths( session, immediateActions );
            }
            return;
        }

        // Check for "EHLO" and "HELO"
        if (cmd.getType() == CommandType.EHLO) {
            logger.debug("Enqueuing private response handler to EHLO command so unknown extensions can be disabled");
            sendCommandToServer( session, cmd, new ResponseCompletion() {
                    /**
                     * Handle response.
                     * @param session AppTCPSession to process.
                     * @param resp    Response to process.
                     */
                    @Override
                    public void handleResponse( AppTCPSession session, Response resp )
                    {
                        logger.debug("Processing response to EHLO Command");
                        sendResponseToClient( session, fixupEHLOResponse(resp, session) );
                    }
                });
            state.heloName = cmd.getArgString();
            followup( session, immediateActions );
            return;
        } else if (cmd.getType() == CommandType.HELO) {
            state.heloName = cmd.getArgString();
            // fall through and continue like before
        }

        if ( state.shutingDownMode ) {
            transactionEnded( session, state.smtpTransactionHandler );
            handleCommandInShutDown( session, cmd );
            return;
        }
        if (state.smtpTransactionHandler != null) {
            if (cmd.getType() == CommandType.RSET) {
                state.smtpTransactionHandler.handleRSETCommand( session, cmd, this  );
                state.smtpTransactionHandler = null;
            } else {
                state.smtpTransactionHandler.handleCommand( session, cmd, this, immediateActions );
            }
        } else {
            // Odd case
            if (cmd.getType() == CommandType.DATA) {
                state.smtpTransactionHandler = getOrCreateTxHandler( state );
                state.smtpTransactionHandler.handleCommand( session, cmd, this, immediateActions );
            } else {

                logger.debug("[handleCommand] with command of type \"" + cmd.getType() + "\"");
                sendCommandToServer( session, cmd, SmtpTransactionHandler.PASSTHRU_RESPONSE_COMPLETION );
            }
        }
        followup( session, immediateActions );
    }

    /**
     * Process command in shutdown.
     * @param session AppTCPSession to process.
     * @param token   Token to process.
     */
    private void handleCommandInShutDown( AppTCPSession session, Token token )
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
                session.sendObjectToClient( new Response(250, "OK") );
                return;
            }
            if (command.getType() == CommandType.NOOP) {
                session.sendObjectToClient( new Response(250, "OK") );
                return;
            }
        }
        send421( session );
    }

    /**
     * Determine if session timed out.
     * @param session AppTCPSession to process.
     * @return        true if session timed out, false otherwise.
     */
    private boolean timedOut( AppTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        if (System.currentTimeMillis() > state.quitAt) {
            sendFINToClient( session );
            return true;
        }
        return false;
    }

    /**
     * Return or create transmission handler.
     * @param  state SmtpSessionState to check.
     * @return       SmtpTransactionHandler intance.
     */
    private SmtpTransactionHandler getOrCreateTxHandler( SmtpSessionState state )
    {
        if (state.smtpTransactionHandler == null) {
            logger.debug("Creating new Transaction Handler");
            state.smtpTransactionHandler = new SmtpTransactionHandler(new SmtpTransaction());
        }
        return state.smtpTransactionHandler;
    }

    /**
     * Process beginning of MIME type.
     * @param session AppTCPSession to process.
     * @param token   BeginMIMEToken to process.
     */
    private void handleBeginMIME( AppTCPSession session, BeginMIMEToken token )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        List<Response> immediateActions = new LinkedList<>();
        state.smtpTransactionHandler = getOrCreateTxHandler( state );
        state.smtpTransactionHandler.handleBeginMIME( session, token, this, immediateActions );
        followup( session, immediateActions );
    }

    /**
     * Process continuation of MIME type.
     * @param session AppTCPSession to process.
     * @param token   ContinuedMIMEToken to process.
     */
    private void handleContinuedMIME( AppTCPSession session, ContinuedMIMEToken token)
    {
        List<Response> immediateActions = new LinkedList<>();
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        SmtpTransactionHandler transactionHandler = getOrCreateTxHandler( state );
        if (token.isLast()) {
            state.smtpTransactionHandler = null;
        }
        transactionHandler.handleContinuedMIME( session, token, this, immediateActions );
        followup( session, immediateActions );
    }

    /**
     * Process response.
     * @param session AppTCPSession to process.
     * @param resp   Response to process.
     */
    private void handleResponse( AppTCPSession session, Response resp)
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        
        logger.debug("[handleResponse()] with code " + resp.getCode());
        if (state.outstandingRequests.size() == 0) {
            long timeDiff = System.currentTimeMillis() - state.timestamp;
            if (timeDiff > LIKELY_TIMEOUT_LENGTH) {
                logger.warn("Unsolicited response from server.  Likely a timeout (" + timeDiff
                        + " millis since last communication)");
            } else {
                logger.info("Response received without a registered handler");
            }
            session.sendObjectToClient( resp );
            return;
        }
        OutstandingRequest or = state.outstandingRequests.remove(0);
        or.getResponseCompletion().handleResponse( session, resp );
        processSynths( session, or.getAdditionalActions() );
    }

    /**
     * Complete mime processing.
     * @param session AppTCPSession to process.
     * @param token   CompleteMIMEToken to process.
     */
    private void handleCompleteMIME( AppTCPSession session, CompleteMIMEToken token )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        SmtpTransactionHandler transactionHandler = getOrCreateTxHandler( state );

        // Looks odd, but the Transaction is complete so just assign the current handler to null.
        state.smtpTransactionHandler = null;
        List<Response> immediateActions = new LinkedList<>();
        transactionHandler.handleCompleteMIME( session, token, this, immediateActions );
        followup( session, immediateActions );
    }

    /**
     * Process TCP client FIN.
     * @param session AppTCPSession to process.
     */
    @Override
    public final void handleTCPClientFIN( AppTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        state.outstandingRequests.add(new OutstandingRequest(SmtpTransactionHandler.NOOP_RESPONSE_COMPLETION));
        logger.debug("Passing along client FIN");
        session.shutdownServer();
    }

    /**
     * Process TCP server FIN.
     * @param session AppTCPSession to process.
     */
    @Override
    public final void handleTCPServerFIN( AppTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        if (!state.shutingDownMode) {
            logger.debug("Passing along server FIN");
            session.shutdownClient();
        } else {
            logger.debug("Supress server FIN");
        }
    }

    /**
     * Process TCP finalized.
     * @param session AppTCPSession to process.
     */
    @Override
    public void handleTCPFinalized( AppTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        if (state.smtpTransactionHandler != null && !state.shutingDownMode) {
            state.smtpTransactionHandler.handleFinalized();
        }
        session.cleanupTempFiles();
    }

    /**
     * Send response to client.
     * @param session AppTCPSession to process.
     * @param resp Response to send.
     */
    public void sendResponseToClient( AppTCPSession session, Response resp )
    {
        logger.debug("Sending response " + resp.getCode() + " to client");
        session.sendObjectToClient( resp );
    }

    /**
     * End transaction.
     * @param session AppTCPSession to process.
     * @param handler SmtpTransactionHandler to use.
     */
    public void transactionEnded( AppTCPSession session, SmtpTransactionHandler handler )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        if (state.smtpTransactionHandler == handler) {
            logger.debug("Deregistering transaction handler");
            state.smtpTransactionHandler = null;
        }
    }

    /**
     * Send FIN to server.
     * @param session AppTCPSession to process.
     * @param compl   ResponseCompletion to send.
     */
    public void sendFINToServer( AppTCPSession session, ResponseCompletion compl )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        logger.debug("Sending FIN to server");
        state.outstandingRequests.add(new OutstandingRequest(compl));
        session.shutdownServer();
    }

    /**
     * Send FIN to client.
     * @param session AppTCPSession to process.
     */
    public void sendFINToClient( AppTCPSession session )
    {
        logger.debug("Sending FIN to client");
        session.shutdownClient();
    }

    /**
     * Send command to server.
     * @param session AppTCPSession to process.
     * @param command Command to send.
     * @param compl   ResponseCompletion to send.
     */
    public void sendCommandToServer( AppTCPSession session, Command command, ResponseCompletion compl )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        logger.debug("Sending Command " + command.getType() + " to server");
        session.sendObjectToServer( command );
        state.outstandingRequests.add(new OutstandingRequest(compl));
    }

    /**
     * Process beginning of MIME type to server.
     * @param session AppTCPSession to process.
     * @param token   BeginMIMEToken to process.
     */
    public void sendBeginMIMEToServer( AppTCPSession session, BeginMIMEToken token )
    {
        logger.debug("Sending BeginMIMEToken to server");
        session.sendObjectToServer( token );
    }

    /**
     * Process continuation of MIME type to server.
     * @param session AppTCPSession to process.
     * @param token   ContinuedMIMEToken to process.
     */
    public void sendContinuedMIMEToServer( AppTCPSession session, ContinuedMIMEToken token )
    {
        logger.debug("Sending intermediate ContinuedMIMEToken to server");
        session.sendObjectToServer( token );
    }

    /**
     * Send final MIME part to server.
     * @param session AppTCPSession to process.
     * @param token   ContinuedMIMEToken to process.
     * @param compl   ResponseCompletion to send.
     */
    public void sendFinalMIMEToServer( AppTCPSession session, ContinuedMIMEToken token, ResponseCompletion compl )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        logger.debug("Sending final ContinuedMIMEToken to server");
        session.sendObjectToServer( token );
        state.outstandingRequests.add(new OutstandingRequest(compl));
    }

    /**
     * Complete mime processing.
     * @param session AppTCPSession to process.
     * @param token   CompleteMIMEToken to process.
     * @param compl ResponseCompletion to send.
     */
    public void sentWholeMIMEToServer( AppTCPSession session, CompleteMIMEToken token, ResponseCompletion compl )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        logger.debug("Sending whole MIME to server");
        session.sendObjectToServer( token );
        state.outstandingRequests.add(new OutstandingRequest(compl));
    }

    /**
     * Appends a response either to the list of outstanding responses, if there are outstanding responses, or to the
     * list of immediate actions, if there are no outstanding responses.
     * 
     * @param session AppTCPSession to process.
     * @param synth Response to send.
     * @param immediateActions List of Response to send.
     * 
     */
    public void appendSyntheticResponse( AppTCPSession session, Response synth, List<Response> immediateActions )
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
     * Get the client IP address
     * @param session AppTCPSession to process.
     * @return Client IP address.
     */
    public InetAddress getClientAddress( AppTCPSession session )
    {
        return session.getClientAddr();
    }

    /**
     * Re-enable the flow of Client tokens. If this method is called while Client Tokens are not
     * {@link #disableClientTokens disabled}, this has no effect.
     * @param session AppTCPSession to process.
     */
    protected void enableClientTokens( AppTCPSession session )
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
     * @param session AppTCPSession to process.
     */
    protected void disableClientTokens( AppTCPSession session )
    {
        SmtpSessionState state = (SmtpSessionState) session.attachment( SESSION_STATE_KEY );
        if (state.clientTokensEnabled) {
            logger.debug("Disabling Client Tokens");
            state.clientTokensEnabled = false;
        } else {
            logger.debug("Redundant call to disable Client Tokens");
        }
    }

    /**
     * Send 421 response.
     * @param session AppTCPSession to process.
     */
    private void send421( AppTCPSession session )
    {
        session.sendObjectToClient( new Response(421, "Service not available, closing transmission channel") );
    }

    /**
     * Block, pass, or modify.
     * @param session AppTCPSession to process.
     * @param  m_msg       MimeMessage to process.
     * @param  transaction SmtpTransaction to process.
     * @param  messageInfo SmtpMessageEvent to process.
     * @return             ScannedMessageResult.
     */
    public abstract ScannedMessageResult blockPassOrModify( AppTCPSession session, MimeMessage m_msg, SmtpTransaction transaction, SmtpMessageEvent messageInfo );

    /**
     * Block or pass.
     * @param session AppTCPSession to process.
     * @param  m_msg       MimeMessage to process.
     * @param  transaction SmtpTransaction to process.
     * @param  messageInfo SmtpMessageEvent to process.
     * @return             BlockOrPassResult
     */
    public abstract BlockOrPassResult blockOrPass( AppTCPSession session, MimeMessage m_msg, SmtpTransaction transaction, SmtpMessageEvent messageInfo );
}
