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

public abstract class SmtpStateMachine extends AbstractTokenHandler
{
    private static final long LIKELY_TIMEOUT_LENGTH = 1000 * 60;// 1 minute

    private static final String[] DEF_ALLOWED_COMMANDS = { "DATA", "HELP", "HELO", "EHLO", "RCPT", "MAIL", "EXPN",
            "QUIT", "RSET", "VRFY", "NOOP", "AUTH" };

    private static final String[] DEF_ALLOWED_EXTENSIONS = { "DATA", "HELP", "HELO", "EHLO", "RCPT", "MAIL", "EXPN",
            "QUIT", "RSET", "VRFY", "NOOP", "SIZE", "DSN", "DELIVERBY", "AUTH", "AUTH=LOGIN", "OK" };

    private final Logger logger = Logger.getLogger(SmtpStateMachine.class);

    private SmtpTransactionHandler smtpTransactionHandler;

    private List<OutstandingRequest> outstandingRequests;

    private Set<String> allowedCommandsSet;

    private String[] allowedExtensionsLC;

    private long clientTimestamp;
    private long serverTimestamp;

    private boolean passthru = false;
    private boolean clientTokensEnabled = true;
    private List<Token> queuedClientTokens = new ArrayList<Token>();

    private boolean scanningEnabled;

    private boolean shutingDownMode = false;
    // Time (absolute) when the class should stop being
    // nice, and nuke the connection with the client
    private long quitAt;

    private final int giveupSz;
    private final long maxClientWait;
    private final long maxServerWait;
    private final boolean isBufferAndTrickle;
    private String heloName = null;

    public SmtpStateMachine(NodeTCPSession session, int giveUpSz, long maxClientWait, long maxServerWait,
            boolean isBufferAndTrickle, boolean scanningEnabled) {
        super(session);
        this.giveupSz = giveUpSz;
        this.maxClientWait = maxClientWait <= 0 ? Integer.MAX_VALUE : maxClientWait;
        this.maxServerWait = maxServerWait <= 0 ? Integer.MAX_VALUE : maxServerWait;
        this.isBufferAndTrickle = isBufferAndTrickle;

        updateTimestamps(true, true);
        this.scanningEnabled = scanningEnabled;

        // Build-out request queue
        outstandingRequests = new LinkedList<OutstandingRequest>();

        allowedCommandsSet = new HashSet<String>();
        for (String command : DEF_ALLOWED_COMMANDS) {
            allowedCommandsSet.add(command.toLowerCase());
        }
        allowedExtensionsLC = new String[DEF_ALLOWED_EXTENSIONS.length];
        for (int i = 0; i < DEF_ALLOWED_EXTENSIONS.length; i++) {
            allowedExtensionsLC[i] = DEF_ALLOWED_EXTENSIONS[i].toLowerCase().trim();
        }

        // The first message passed for SMTP is actualy from the server.
        // Place a Response handler into the OutstandingRequest queue to handle this and call our SessionHandler with
        // the initial salutation
        outstandingRequests.add(new OutstandingRequest(new ResponseCompletion()
        {
            @Override
            public void handleResponse(Response resp, TokenResultBuilder ts)
            {
                handleOpeningResponse(resp, ts);
            }
        }));
    }

    /**
     * Get the size over-which this class should no longer buffer. After this point, the Buffering is abandoned and the
     * <i>giveup-then-trickle</i> state is entered.
     */
    protected final int getGiveupSz()
    {
        return giveupSz;
    }

    /**
     * The maximum time (in relative milliseconds) that the client can wait for a response to DATA transmission.
     */
    protected final long getMaxClientWait()
    {
        return maxClientWait;
    }

    /**
     * The maximum time that the server can wait for a subsequent ("DATA") command.
     */
    protected final long getMaxServerWait()
    {
        return maxServerWait;
    }

    /**
     * If true, this handler will continue to buffer even after trickling has begun (<i>buffer-and-trickle</i> mode).
     * The MIMEMessage can no longer be modified (i.e. it will not be passed downstream) but {@link #blockOrPass
     * blockOrPass()} will still be called once the complete message has been seen.
     */
    protected final boolean isBufferAndTrickle()
    {
        return isBufferAndTrickle;
    }

    /**
     * Determines, based on timestamps, if trickling should begin
     */
    protected boolean shouldBeginTrickle()
    {

        long maxWaitPeriod = Math.min(getMaxClientWait(), getMaxServerWait());
        long lastTimestamp = Math.min(getLastClientTimestamp(), getLastServerTimestamp());
        if (maxWaitPeriod <= 0) {
            // Time equal-to or below zero means give-up
            return false;
        }
        maxWaitPeriod = (long) (maxWaitPeriod * 0.95);// TODO bscott a real "slop" factor - not a guess

        return (System.currentTimeMillis() - lastTimestamp) < maxWaitPeriod ? false : true;
    }

    private final void updateTimestamps(boolean client, boolean server)
    {
        long now = System.currentTimeMillis();
        if (client) {
            clientTimestamp = now;
        }
        if (server) {
            serverTimestamp = now;
        }
    }

    public void startShutingDown()
    {
        shutingDownMode = true;
    }

    public boolean isShutingDown()
    {
        return shutingDownMode;
    }

    @Override
    public TokenResult handleClientToken(Token token) throws TokenException
    {
        updateTimestamps(true, false);

        TokenResultBuilder trb = new TokenResultBuilder();

        // First add the token, to preserve ordering if we have a queue (and while draining someone changes the
        // enablement flag)
        queuedClientTokens.add(token);

        if (!clientTokensEnabled) {
            logger.debug("[handleClientToken] Queuing Token \"" + token.getClass().getName() + "\" ("
                    + queuedClientTokens.size() + " tokens queued)");
        } else {
            // Important - the enablement of client tokens could change as this loop is running.
            while (queuedClientTokens.size() > 0 && clientTokensEnabled) {
                if (queuedClientTokens.size() > 1) {
                    logger.debug("[handleClientToken] Draining Queued Token \""
                            + queuedClientTokens.get(0).getClass().getName() + "\" (" + queuedClientTokens.size()
                            + " tokens remain)");
                }
                handleClientTokenImpl(queuedClientTokens.remove(0), trb);
            }
        }
        if (queuedClientTokens.size() > 0) {
            logger.debug("[handleClientToken] returning with (" + queuedClientTokens.size() + " queued tokens)");
        }
        updateTimestamps(trb.hasDataForClient(), trb.hasDataForServer());
        return trb.getTokenResult();
    }

    @Override
    public TokenResult handleServerToken(Token token) throws TokenException
    {
        updateTimestamps(false, true);

        TokenResultBuilder trb = new TokenResultBuilder();

        while (queuedClientTokens.size() > 0 && clientTokensEnabled) {
            logger.debug("[handleServerToken] Draining Queued Client Token \""
                    + queuedClientTokens.get(0).getClass().getName() + "\" (" + queuedClientTokens.size()
                    + " tokens remain)");
            handleClientTokenImpl(queuedClientTokens.remove(0), trb);
        }
        handleServerTokenImpl(token, trb);

        // Important - the enablement of client tokens could change as this loop is running.
        while (queuedClientTokens.size() > 0 && clientTokensEnabled) {
            logger.debug("[handleServerToken] Draining Queued Token \""
                    + queuedClientTokens.get(0).getClass().getName() + "\" (" + queuedClientTokens.size()
                    + " tokens remain)");
            handleClientTokenImpl(queuedClientTokens.remove(0), trb);
        }
        if (queuedClientTokens.size() > 0) {
            logger.debug("[handleServerToken] returning with (" + queuedClientTokens.size() + " queued tokens)");
        }
        updateTimestamps(trb.hasDataForClient(), trb.hasDataForServer());
        return trb.getTokenResult();
    }

    // FROM Client
    private final void handleClientTokenImpl(Token token, TokenResultBuilder trb) throws TokenException
    {
        // Check for passthrough
        if (passthru || !scanningEnabled) {
            logger.debug("(In passthru, client token) passing token of type " + token.getClass().getName());
            trb.addTokenForServer(token);
            return;
        }
        if (token instanceof SASLExchangeToken || token instanceof AUTHCommand || token instanceof PassThruToken) {
            logger.debug("Received " + token.getClass().getName() + " token");
            if (token instanceof PassThruToken) {
                passthru = true;
                logger.debug("(client token) Entering Passthru");
            }
            trb.addTokenForServer(token);
            return;
        }
        if (token instanceof CommandWithEmailAddress && !shutingDownMode) {
            smtpTransactionHandler = getOrCreateTxHandler();
            if (((CommandWithEmailAddress) token).getType() == CommandType.MAIL)
                smtpTransactionHandler.handleMAILCommand((CommandWithEmailAddress) token, this, trb);
            else
                smtpTransactionHandler.handleRCPTCommand((CommandWithEmailAddress) token, this, trb);
            return;
        }
        if (token instanceof Command) {
            handleCommand(trb, (Command) token);
            return;
        }
        if (token instanceof Chunk) {
            trb.addTokenForServer((Chunk) token);
            return;
        }
        // the rest of the commands are handled differently if in shutdown mode
        if (shutingDownMode) {
            handleCommandInShutDown(token, trb);
            return;
        }
        if (token instanceof BeginMIMEToken) {
            handleBeginMIME(trb, (BeginMIMEToken) token);
            return;
        }
        if (token instanceof ContinuedMIMEToken) {
            handleContinuedMIME(trb, (ContinuedMIMEToken) token);
            return;
        }
        if (token instanceof CompleteMIMEToken) {
            handleCompleteMIME(trb, (CompleteMIMEToken) token);
            return;
        }
        logger.error("(client token) Unexpected Token of type \"" + token.getClass().getName() + "\".  Pass it along");
        trb.addTokenForServer(token);
    }

    // FROM Server
    private final void handleServerTokenImpl(Token token, TokenResultBuilder trb) throws TokenException
    {
        if (passthru || !scanningEnabled) {
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
            passthru = true;
            trb.addTokenForClient(token);
            return;
        } else if (token instanceof Response) {
            handleResponse(trb, (Response) token);
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
    public void handleOpeningResponse(Response resp, TokenResultBuilder ts)
    {
        if (shutingDownMode) {
            if (timedOut()) {
                return;
            } else {
                send421(ts);
            }
        } else {
            sendResponseToClient(resp, ts);
        }
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
            if (isAllowedExtension(verb)) {
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
     * Scans the allowed extension list for the presence of "extensionName"
     */
    private boolean isAllowedExtension(String extensionName)
    {
        // Thread safety
        String[] allowedExtensionsLC = this.allowedExtensionsLC;
        extensionName = extensionName.toLowerCase();
        for (String permitted : allowedExtensionsLC) {
            if (extensionName.equals(permitted)) {
                return true;
            }
        }
        return false;
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

    private void handleCommand(TokenResultBuilder resultBuilder, Command cmd)
    {

        logger.debug("Received Command \"" + cmd.getCmdString() + "\"" + " of type \"" + cmd.getClass().getName()
                + "\"");
        List<Response> immediateActions = new LinkedList<Response>();

        // Check for allowed commands
        String cmdStrLower = cmd.getCmdString().toLowerCase();
        if ((!(cmd instanceof UnparsableCommand)) && !allowedCommandsSet.contains(cmdStrLower)) {
            logger.warn("Enqueuing negative response to " + "non-allowed command \"" + cmd.getCmdString() + "\"" + " ("
                    + cmd.getArgString() + ")");
            appendSyntheticResponse(new Response(500, "Syntax error, command unrecognized"), immediateActions);
            if (immediateActions.size() > 0) {
                processSynths(immediateActions, resultBuilder);
            }
            return;
        }

        // Check for "EHLO" and "HELO"
        if (cmd.getType() == CommandType.EHLO) {
            logger.debug("Enqueuing private response handler to EHLO command so unknown extensions can be disabled");
            sendCommandToServer(cmd, new ResponseCompletion()
            {
                @Override
                public void handleResponse(Response resp, TokenResultBuilder ts)
                {
                    logger.debug("Processing response to EHLO Command");
                    sendResponseToClient(fixupEHLOResponse(resp), ts);
                }
            }, resultBuilder);
            heloName = cmd.getArgString();
            followup(immediateActions, resultBuilder);
            return;
        } else if (cmd.getType() == CommandType.HELO) {
            heloName = cmd.getArgString();
            // fall through and continue like before
        }

        if (shutingDownMode) {
            transactionEnded(smtpTransactionHandler);
            handleCommandInShutDown(cmd, resultBuilder);
            return;
        }
        if (smtpTransactionHandler != null) {
            if (cmd.getType() == CommandType.RSET) {
                smtpTransactionHandler.handleRSETCommand(cmd, this, resultBuilder);
                smtpTransactionHandler = null;
            } else {
                smtpTransactionHandler.handleCommand(cmd, this, resultBuilder, immediateActions);
            }
        } else {
            // Odd case
            if (cmd.getType() == CommandType.DATA) {
                smtpTransactionHandler = getOrCreateTxHandler();
                smtpTransactionHandler.handleCommand(cmd, this, resultBuilder, immediateActions);
            } else {

                logger.debug("[handleCommand] with command of type \"" + cmd.getType() + "\"");
                sendCommandToServer(cmd, SmtpTransactionHandler.PASSTHRU_RESPONSE_COMPLETION, resultBuilder);
            }
        }
        followup(immediateActions, resultBuilder);
    }

    private void handleCommandInShutDown(Token token, TokenResultBuilder ts)
    {
        // Check for our timeout
        if (timedOut()) {
            return;
        }
        if (token instanceof CommandWithEmailAddress || token instanceof Command) {
            Command command = (Command) token;
            // Check for "special" commands
            if (command.getType() == CommandType.QUIT) {
                sendFINToClient();
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

    private boolean timedOut()
    {
        if (System.currentTimeMillis() > quitAt) {
            sendFINToClient();
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

    private void handleBeginMIME(TokenResultBuilder resultBuilder, BeginMIMEToken token)
    {
        List<Response> immediateActions = new LinkedList<Response>();
        smtpTransactionHandler = getOrCreateTxHandler();
        smtpTransactionHandler.handleBeginMIME(token, this, immediateActions, resultBuilder);
        followup(immediateActions, resultBuilder);
    }

    private void handleContinuedMIME(TokenResultBuilder resultBuilder, ContinuedMIMEToken token)
    {
        List<Response> immediateActions = new LinkedList<Response>();
        SmtpTransactionHandler transactionHandler = getOrCreateTxHandler();
        if (token.isLast()) {
            smtpTransactionHandler = null;
        }
        transactionHandler.handleContinuedMIME(token, this, immediateActions, resultBuilder);
        followup(immediateActions, resultBuilder);
    }

    private void handleResponse(TokenResultBuilder resultBuilder, Response resp)
    {
        logger.debug("[handleResponse()] with code " + resp.getCode());
        if (outstandingRequests.size() == 0) {
            long timeDiff = System.currentTimeMillis() - getLastServerTimestamp();
            if (timeDiff > LIKELY_TIMEOUT_LENGTH) {
                logger.warn("Unsolicited response from server.  Likely a timeout (" + timeDiff
                        + " millis since last communication)");
            } else {
                logger.info("Response received without a registered handler");
            }
            resultBuilder.addTokenForClient(resp);
            return;
        }
        OutstandingRequest or = outstandingRequests.remove(0);
        or.getResponseCompletion().handleResponse(resp, resultBuilder);
        processSynths(or.getAdditionalActions(), resultBuilder);
    }

    private void handleCompleteMIME(TokenResultBuilder resultBuilder, CompleteMIMEToken token)
    {

        SmtpTransactionHandler transactionHandler = getOrCreateTxHandler();

        // Looks odd, but the Transaction is complete so just assign the current handler to null.
        smtpTransactionHandler = null;
        List<Response> immediateActions = new LinkedList<Response>();
        transactionHandler.handleCompleteMIME(token, this, immediateActions, resultBuilder);
        followup(immediateActions, resultBuilder);
    }

    public final void handleClientFin() throws TokenException
    {
        outstandingRequests.add(new OutstandingRequest(SmtpTransactionHandler.NOOP_RESPONSE_COMPLETION));
        logger.debug("Passing along client FIN");
        getSession().shutdownServer();
    }

    public final void handleServerFin() throws TokenException
    {
        if (!shutingDownMode) {
            logger.debug("Passing along server FIN");
            getSession().shutdownClient();
        } else {
            logger.debug("Supress server FIN");
        }
    }

    public void handleFinalized()
    {
        if (smtpTransactionHandler != null && !shutingDownMode) {
            smtpTransactionHandler.handleFinalized();
        }
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

    public void sendFINToServer(ResponseCompletion compl)
    {
        logger.debug("Sending FIN to server");
        outstandingRequests.add(new OutstandingRequest(compl));
        getSession().shutdownServer();
    }

    public void sendFINToClient()
    {
        logger.debug("Sending FIN to client");
        getSession().shutdownClient();
    }

    public void sendCommandToServer(Command command, ResponseCompletion compl, TokenResultBuilder ts)
    {
        logger.debug("Sending Command " + command.getType() + " to server");
        ts.addTokenForServer(command);
        outstandingRequests.add(new OutstandingRequest(compl));
    }

    public void sendBeginMIMEToServer(BeginMIMEToken token, TokenResultBuilder ts)
    {
        logger.debug("Sending BeginMIMEToken to server");
        ts.addTokenForServer(token);
    }

    public void sendContinuedMIMEToServer(ContinuedMIMEToken token, TokenResultBuilder ts)
    {
        logger.debug("Sending intermediate ContinuedMIMEToken to server");
        ts.addTokenForServer(token);
    }

    public void sendFinalMIMEToServer(ContinuedMIMEToken token, ResponseCompletion compl, TokenResultBuilder ts)
    {
        logger.debug("Sending final ContinuedMIMEToken to server");
        ts.addTokenForServer(token);
        outstandingRequests.add(new OutstandingRequest(compl));
    }

    public void sentWholeMIMEToServer(CompleteMIMEToken token, ResponseCompletion compl, TokenResultBuilder ts)
    {
        logger.debug("Sending whole MIME to server");
        ts.addTokenForServer(token);
        outstandingRequests.add(new OutstandingRequest(compl));
    }

    /**
     * Appends a response either to the list of outstanding responses, if there are outstanding responses, or to the
     * list of immediate actions, if there are no outstanding responses.
     * 
     * @param synth
     * @param immediateActions
     * 
     */
    public void appendSyntheticResponse(Response synth, List<Response> immediateActions)
    {
        logger.debug("Appending synthetic response");
        if (outstandingRequests.size() == 0) {
            immediateActions.add(synth);
        } else {
            outstandingRequests.get(outstandingRequests.size() - 1).getAdditionalActions().add(synth);
        }
    }

    /**
     * Get the absolute time (based on the local clock) of when the client last sent or was-sent a unit of data.
     */
    public long getLastClientTimestamp()
    {
        return clientTimestamp;
    }

    /**
     * Get the absolute time (based on the local clock) of when the server last was sent or sent a unit of data.
     */
    public long getLastServerTimestamp()
    {
        return serverTimestamp;
    }

    /**
     * Get the client IP address
     */
    public InetAddress getClientAddress()
    {
        return getSession().getClientAddr();
    }

    /**
     * Re-enable the flow of Client tokens. If this method is called while Client Tokens are not
     * {@link #disableClientTokens disabled}, this has no effect.
     */
    protected void enableClientTokens()
    {
        if (!clientTokensEnabled) {
            logger.debug("Re-enabling Client Tokens");
            clientTokensEnabled = true;
        } else {
            logger.debug("Redundant call to enable Client Tokens");
        }
    }

    /**
     * Disable the flow of client tokens. No more calls to the Handler will be made with Client Tokens until the
     * {@link #enableClientTokens enable method} is called.
     */
    protected void disableClientTokens()
    {
        if (clientTokensEnabled) {
            logger.debug("Disabling Client Tokens");
            clientTokensEnabled = false;
        } else {
            logger.debug("Redundant call to disable Client Tokens");
        }
    }

    private void send421(TokenResultBuilder ts)
    {
        ts.addTokenForClient(new Response(421, "Service not available, closing transmission channel"));
    }

    public String getHeloName()
    {
        return heloName;
    }

    public abstract ScannedMessageResult blockPassOrModify(MimeMessage m_msg, SmtpTransaction transaction,
            MessageInfo m_messageInfo);

    public abstract BlockOrPassResult blockOrPass(MimeMessage m_msg, SmtpTransaction transaction,
            MessageInfo m_messageInfo);
}
