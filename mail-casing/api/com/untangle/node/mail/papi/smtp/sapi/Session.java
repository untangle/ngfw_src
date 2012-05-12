/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.mail.papi.smtp.sapi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.BeginMIMEToken;
import com.untangle.node.mail.papi.CompleteMIMEToken;
import com.untangle.node.mail.papi.ContinuedMIMEToken;
import com.untangle.node.mail.papi.smtp.Command;
import com.untangle.node.mail.papi.smtp.MAILCommand;
import com.untangle.node.mail.papi.smtp.RCPTCommand;
import com.untangle.node.mail.papi.smtp.Response;
import com.untangle.node.mail.papi.smtp.SmtpTokenStream;
import com.untangle.node.mail.papi.smtp.SmtpTokenStreamHandler;
import com.untangle.node.mail.papi.smtp.SmtpTransaction;
import com.untangle.node.mail.papi.smtp.UnparsableCommand;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.TokenResultBuilder;
import com.untangle.uvm.vnet.NodeTCPSession;


/**
 * Class which acts to listen on an SMTP Token Stream
 * and convert to a more managable
 * {@link com.untangle.node.mail.papi.smtp.sapi.Session Session-oriented}
 * API.
 * <br><br>
 * The primary benifit of this class is that it programatically
 * enforces transaction boundaries, and provides a queue to allign
 * requests/responses while permitting (a) protocol manipulation
 * and (b) pipelining.
 *
 * @see com.untangle.node.mail.papi.smtp.sapi.Session
 */
public final class Session
    extends SmtpTokenStream {


    //==========================
    // Inner-Interfaces
    //===========================

    /**
     * Base class of available Actions to take
     * during various SMTP events.  Instances
     * of SmtpActions are passed to callbacks
     * on {@link com.untangle.node.mail.papi.smtp.sapi.SyntheticAction SyntheticAction}s
     */
    public interface SmtpActions {

        /**
         * Cause client tokens to be queued before this
         * node (and ultimatly perhaps to the client).
         */
        public void disableClientTokens();

        /**
         * Corresponding re-enablement method for the
         * {@link #disableClientTokens disableClientTokens} method.  Note
         * that this may be called even if the client is not disabled.
         */
        public void enableClientTokens();

        /**
         * Callback indicating that the transaction has ended.  Normally,
         * the Session knows this based on the input from the client.  However,
         * if manipulation is taking place the Session may not be able to detect
         * this.  As such, it is a "good idea" to always call this method.
         */
        public void transactionEnded(TransactionHandler handler);

        /**
         * Send a FIN to the server (i.e. shutdown the server-half).
         * Note that this takes place as soon as the call is made
         * (i.e. possibly trapping any tokens headed for the server);
         *
         * The ResponseCompletion is added in case the goofy
         * server sends an acknowledgement to the response.
         */
        public void sendFINToServer(ResponseCompletion compl);

        /**
         * Send a FIN to the client (i.e. shutdown the client-half).
         * Note that this takes place as soon as the call is made
         * (i.e. possibly trapping any tokens headed for the client);
         */
        public void sendFINToClient();

        /**
         * Send a Command to the server
         *
         * @param command the command
         * @param compl the completion to be called when the response
         *        to <b>this</b> command returns from the server
         */
        public void sendCommandToServer(Command command,
                                        ResponseCompletion compl);

        /**
         * Send the start of a MIME message to the server.
         * <br><br>
         * Note that this does not have a corresponding
         * ResponseCompletion because there is no expected
         * response to a <i>portion</i> of the message
         *
         * @param token the token
         */
        public void sendBeginMIMEToServer(BeginMIMEToken token);

        /**
         * Send a non-end MIME token to the server.
         * <br><br>
         * Note that this does not have a corresponding
         * ResponseCompletion because there is no expected
         * response to a <i>portion</i> of the message
         *
         * @param token the continued MIME token
         */
        public void sendContinuedMIMEToServer(ContinuedMIMEToken token);

        /**
         * Send the remaining MIME content to the server.
         *
         * @param token the last MIME token
         * @param compl the completion to be called when the response
         *        to <i>this</i> data transmission returns from the server
         */
        public void sendFinalMIMEToServer(ContinuedMIMEToken token,
                                          ResponseCompletion compl);

        /**
         * Send a complete MIME message to the server
         *
         * @param token the message
         * @param compl the completion to be called when the response
         *        to <i>this</i> data transmission returns from the server
         */
        public void sentWholeMIMEToServer(CompleteMIMEToken token,
                                          ResponseCompletion compl);



    }

    /**
     * Callback interface for
     * {@link com.untangle.node.mail.papi.smtp.sapi.SessionHandler Sessions}
     * or {@link com.untangle.node.mail.papi.smtp.sapi.TransactionHandler Transactions}
     * to take action when an SMTP command arrives.
     */
    public interface SmtpCommandActions
        extends SmtpActions {

        /**
         * Append a synthetic response.  This is used when the
         * Handler has decided not to pass-along a client
         * request to the server.  This causes the SyntheticAction
         * to be placed within the internal Outstanding Request
         * queue such that the Synthetic response will be issued
         * in the correct order.
         * <br>
         * If the handler using this Object makes this call and there
         * are currently no outstanding requests, at the completion
         * of the handler callback the <code>synth</code>
         * will be called to issue the "fake" response (with
         * the same TokenStreamer, to preserve ordering).
         */
        public void appendSyntheticResponse(SyntheticResponse synth);


        /**
         * This is a <b>really</b> specialized method, only to be used
         * if you know what you're doing.  This will send a response
         * to the client <b>ignoring</b> the queue of outstanding responses.
         * <br><br>
         * This is to be used <b>only</b> when the server has been discarded,
         * but may send one more obnoxious "bye" message.
         */
        public void sendResponseNow(Response response);

    }

    /**
     * Set of actions available to
     * {@link com.untangle.node.mail.papi.smtp.sapi.ResponseCompletion ResponseCompletion}
     * instances while they are called-back.
     */
    public interface SmtpResponseActions
        extends SmtpActions {

        /**
         * Send a Response to the client.
         *
         * @param resp the response
         */
        public void sendResponseToClient(Response resp);

    }


    //==========================
    // Data Members
    //===========================

    private static final long LIKELY_TIMEOUT_LENGTH = 1000*60;//1 minute

    private static final String[] DEF_ALLOWED_COMMANDS = {
        "DATA",
        "HELP",
        "HELO",
        "EHLO",
        "RCPT",
        "MAIL",
        "EXPN",
        "QUIT",
        "RSET",
        "VRFY",
        "NOOP",
        "STARTTLS",		// Dynamically removed later if !allowTLS
        "AUTH"
    };

    private static final String[] DEF_ALLOWED_EXTENSIONS = {
        "DATA",
        "HELP",
        "HELO",
        "EHLO",
        "RCPT",
        "MAIL",
        "EXPN",
        "QUIT",
        "RSET",
        "VRFY",
        "NOOP",
        "SIZE",
        //    "PIPELINING",
        "STARTTLS",		// Dynamically removed later if !allowTLS
        "DSN",
        "DELIVERBY",
        "AUTH",
        "AUTH=LOGIN",
        "OK"//Added in case they just send "250 OK"
    };

    private final Logger m_logger = Logger.getLogger(Session.class);

    private MySmtpTokenStreamHandler m_streamHandler =
        new MySmtpTokenStreamHandler();

    private SessionHandler m_sessionHandler;//Sink
    private TransactionHandler m_currentTxHandler;//Sink

    private List<OutstandingRequest> m_outstandingRequests;

    private Set<String> m_allowedCommandsSet;
    private String[] m_allowedCommands;
    private String[] m_allowedExtensionsLC;
    private String[] m_allowedExtensions;





    //==========================
    // Construction
    //===========================

    /**
     * Construct a new Session
     *
     * @param session the NodeTCPSession to listen-on
     * @param handler the Session handler
     */
    public Session(NodeTCPSession session,
                   SessionHandler handler,
		   boolean allowTLS) {
        super(session);

        //Set out fixed StreamHandler as
        //the listener for Stream events
        super.setHandler(m_streamHandler);

        //Install defaults for permitted
        //commands/extensions
	String[] allowedCommands, allowedExtensions;
	if (allowTLS) {
	    allowedCommands = DEF_ALLOWED_COMMANDS;
	    allowedExtensions = DEF_ALLOWED_EXTENSIONS;
	} else {
	    int j = 0;
	    // don't allow TLS
	    allowedCommands = new String[DEF_ALLOWED_COMMANDS.length - 1];
	    allowedExtensions = new String[DEF_ALLOWED_EXTENSIONS.length - 1];
	    for (String com : DEF_ALLOWED_COMMANDS)
		if (!com.equals("STARTTLS"))
		    allowedCommands[j++] = com;
	    j = 0;
	    for (String ext : DEF_ALLOWED_EXTENSIONS)
		if (!ext.equals("STARTTLS"))
		    allowedExtensions[j++] = ext;
	}
        setAllowedCommands(allowedCommands);
        setAllowedExtensions(allowedExtensions);

        //Assign the handler
        setSessionHandler(handler);

        //Build-out request queue
        m_outstandingRequests = new LinkedList<OutstandingRequest>();

        //Tricky little thing here.  The first message passed for SMTP
        //is actualy from the server.  Place a Response handler
        //into the OutstandingRequest queue to handle this and
        //call our SessionHandler with the initial salutation
        m_outstandingRequests.add(new OutstandingRequest(
                                                         new ResponseCompletion() {
                                                             public void handleResponse(Response resp,
                                                                                        Session.SmtpResponseActions actions) {
                                                                 m_sessionHandler.handleOpeningResponse(resp, actions);
                                                             }
                                                         }));

    }

    /**
     * Create a Session which will simply pass-thru all
     * commands (yet continue to do any SMTP fixup goodness).
     *
     * @param session the NodeTCPSession
     *
     * @return the passthru session.
     */
    public static Session createPassthruSession(NodeTCPSession session)
    {
        return new Session(session, new SimpleSessionHandler(), true);
    }


    //==========================
    // Properties
    //===========================

    /**
     * Set the instance which will receive callbacks
     * around interesting portions of an SMTP session
     *
     * @param handler the handler (not tolerant of null).
     */
    public void setSessionHandler(SessionHandler handler) {
        m_sessionHandler = handler;
        m_sessionHandler.setSession(this);
    }

    /**
     * Set the list of permitted commands (e.g. "EHLO", "DATA").  Note
     * that this method is not smart, and you could pass-in something
     * stupid like an array w/o "HELO".
     *
     * @param commands the permitted commands
     */
    public void setAllowedCommands(String[] commands) {
        Set<String> newAllowedCommandsSet = new HashSet<String>();
        for(String command : commands) {
            newAllowedCommandsSet.add(command.toLowerCase());
        }
        m_allowedCommandsSet = newAllowedCommandsSet;
        m_allowedCommands = commands;
    }

    /**
     * Get the list of {@link setAllowedCommands Allowed COmmands}
     */
    public String[] getAllowedCommands() {
        return m_allowedCommands;
    }

    /**
     * Set the list of permitted extensions.  This class
     * is dumb, so it will not detect if you set this to
     * a blank list.  It also does not imply that these
     * extensions will be advertized, only that these
     * will <b>not</b> be stripped from an EHLO
     * response.
     *
     *
     *
     * @param commands the permitted extensions
     */
    public void setAllowedExtensions(String[] extensions) {
        String[] newAllowedExtensionsLC = new String[extensions.length];

        for(int i = 0; i<extensions.length; i++) {
            newAllowedExtensionsLC[i] = extensions[i].toLowerCase().trim();
        }
        m_allowedExtensionsLC = newAllowedExtensionsLC;
        m_allowedExtensions = extensions;
    }

    /**
     * Get the list of {@link #setAllowedExtensions Allowed Extensions}
     */
    public String[] getAllowedExtensions() {
        return m_allowedExtensions;
    }



    //==========================
    // Helpers
    //===========================

    /**
     * Process any Synthetic actions
     */
    private void processSynths(HoldsSyntheticActions synths,
                               TokenResultBuilder trb) {

        SmtpResponseActionsImpl actions = new SmtpResponseActionsImpl(trb);
        SyntheticResponse action = synths.popAction();
        while(action != null) {
            action.handle(actions);
            action = synths.popAction();
        }
    }

    /**
     * Helper
     */
    private TransactionHandler getOrCreateTxHandler() {
        if(m_currentTxHandler == null) {
            m_logger.debug("Creating new Transaction Handler");
            m_currentTxHandler = m_sessionHandler.createTxHandler(new SmtpTransaction());
        }
        return m_currentTxHandler;
    }

    /**
     * Method which removes any unknown ESMTP extensions
     */
    private Response fixupEHLOResponse(Response resp) {

        String[] respLines = resp.getArgs();
        if(respLines == null || respLines.length < 2) {
            //Note first line is the "doman" junk
            return resp;
        }



        List<String> finalList = new ArrayList<String>();
        finalList.add(respLines[0]);//Add domain line

        for(int i = 1; i<respLines.length; i++) {
            String verb = getCapabilitiesLineVerb(respLines[i]);
            if(isAllowedExtension(verb)) {
                m_logger.debug("Allowing ESMTP response line \"" + respLines[i] +
                               "\" to go to client");
                finalList.add(respLines[i]);
            }
            else {
                m_logger.debug("Removing unknown extension \"" + respLines[i] + "\" (" + verb + ")");
            }
        }

        String[] newRespLines = finalList.toArray(new String[finalList.size()]);
        return new Response(resp.getCode(), newRespLines);
    }

    /**
     * Scans the allowed extension list for the
     * presence of "extensionName"
     */
    private boolean isAllowedExtension(String extensionName) {
        //Thread safety
        String[] allowedExtensionsLC = m_allowedExtensionsLC;
        extensionName = extensionName.toLowerCase();
        for(String permitted : allowedExtensionsLC) {
            if(extensionName.equals(permitted)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Picks the verb out-of an ESMTP response
     * line (i.e. "SIZE" out of "SIZE 13546654").
     */
    private String getCapabilitiesLineVerb(String str) {
        //TODO bscott.  Are all commands separated
        //     by a space from any arguments?
        str = str.trim();
        int index = str.indexOf(' ');
        if(index < 0) {
            return str;
        }
        return str.substring(0, index);
    }



    //==========================
    // Inner-Classes
    //===========================


    //=============== Inner Class Separator ====================

    private class SmtpActionsImpl
        implements SmtpActions {

        private final TokenResultBuilder m_ts;

        SmtpActionsImpl(TokenResultBuilder ts) {
            m_ts = ts;
        }
        public TokenResultBuilder getTokenResultBuilder() {
            return m_ts;
        }
        //    private void enqueueResponseHandler(ResponseCompletion cont) {
        //      m_outstandingRequests.add(new OutstandingRequest(cont));
        //    }
        public void disableClientTokens() {
            Session.this.disableClientTokens();
        }

        public void enableClientTokens() {
            Session.this.enableClientTokens();
        }
        public void transactionEnded(TransactionHandler handler) {
            if(m_currentTxHandler == handler) {
                m_logger.debug("Deregistering transaction handler");
                m_currentTxHandler = null;
            }
        }
        public void sendFINToServer(ResponseCompletion compl) {
            m_logger.debug("Sending FIN to server");
            m_outstandingRequests.add(new OutstandingRequest(compl));
            Session.this.getSession().shutdownServer();
        }

        public void sendFINToClient() {
            m_logger.debug("Sending FIN to client");
            Session.this.getSession().shutdownClient();
        }

        public void sendCommandToServer(Command command,
                                        ResponseCompletion compl) {
            m_logger.debug("Sending Command " + command.getType() + " to server");
            getTokenResultBuilder().addTokenForServer(command);
            m_outstandingRequests.add(new OutstandingRequest(compl));
        }
        public void sendBeginMIMEToServer(BeginMIMEToken token) {
            m_logger.debug("Sending BeginMIMEToken to server");
            getTokenResultBuilder().addTokenForServer(token);
        }
        public void sendContinuedMIMEToServer(ContinuedMIMEToken token) {
            m_logger.debug("Sending intermediate ContinuedMIMEToken to server");
            getTokenResultBuilder().addTokenForServer(token);
        }
        public void sendFinalMIMEToServer(ContinuedMIMEToken token,
                                          ResponseCompletion compl) {
            m_logger.debug("Sending final ContinuedMIMEToken to server");
            getTokenResultBuilder().addTokenForServer(token);
            m_outstandingRequests.add(new OutstandingRequest(compl));
        }
        public void sentWholeMIMEToServer(CompleteMIMEToken token,
                                          ResponseCompletion compl) {
            m_logger.debug("Sending whole MIME to server");
            getTokenResultBuilder().addTokenForServer(token);
            m_outstandingRequests.add(new OutstandingRequest(compl));
        }

    }



    //=============== Inner Class Separator ====================

    private class SmtpResponseActionsImpl
        extends SmtpActionsImpl
        implements SmtpResponseActions {

        SmtpResponseActionsImpl(TokenResultBuilder ts) {
            super(ts);
        }
        public void sendResponseToClient(Response resp) {
            m_logger.debug("Sending response " + resp.getCode() + " to client");
            getTokenResultBuilder().addTokenForClient(resp);
        }

    }



    //=============== Inner Class Separator ====================

    private class SmtpCommandActionsImpl
        extends SmtpActionsImpl
        implements SmtpCommandActions {

        private HoldsSyntheticActions m_immediateActions = null;

        SmtpCommandActionsImpl(TokenResultBuilder ts) {
            super(ts);
        }

        public void sendResponseNow(Response response) {
            getTokenResultBuilder().addTokenForClient(response);
        }

        public void appendSyntheticResponse(SyntheticResponse synth) {
            m_logger.debug("Appending synthetic response");
            if(m_outstandingRequests.size() == 0) {
                if(m_immediateActions == null) {
                    m_immediateActions = new HoldsSyntheticActions();
                }
                m_immediateActions.pushAction(synth);
            }
            else {
                m_outstandingRequests.get(m_outstandingRequests.size()-1).pushAction(synth);
            }
        }
        void followup() {
            if(m_immediateActions != null) {
                processSynths(m_immediateActions, getTokenResultBuilder());
            }
        }
    }


    //=============== Inner Class Separator ====================

    private class HoldsSyntheticActions {
        private List<SyntheticResponse> m_additionalActions;

        void pushAction(SyntheticResponse synth) {
            if(m_additionalActions == null) {
                m_additionalActions = new LinkedList<SyntheticResponse>();
            }
            m_additionalActions.add(synth);
        }
        //Must keep calling this until it returns null, as
        //one synthetic may enqueue another
        SyntheticResponse popAction() {
            if(m_additionalActions == null || m_additionalActions.size() == 0) {
                return null;
            }
            return m_additionalActions.remove(0);
        }
    }



    //=============== Inner Class Separator ====================

    private class OutstandingRequest
        extends HoldsSyntheticActions {

        final ResponseCompletion cont;

        OutstandingRequest(ResponseCompletion cont) {
            this.cont = cont;
        }

    }


    //=============== Inner Class Separator ====================

    private class MySmtpTokenStreamHandler
        extends SmtpTokenStreamHandler {

        @Override
        public void passthru(TokenResultBuilder resultBuilder) {
            //Nothing to do
        }

        @Override
        public void handleCommand(TokenResultBuilder resultBuilder,
                                  Command cmd) {

            m_logger.debug("Received Command \"" + cmd.getCmdString() + "\"" +
                           " of type \"" + cmd.getClass().getName() + "\"");

            SmtpCommandActionsImpl actions = new SmtpCommandActionsImpl(resultBuilder);

            //Check for allowed commands
            String cmdStrLower = cmd.getCmdString().toLowerCase();
            if(
               (!(cmd instanceof UnparsableCommand)) &&
               !m_allowedCommandsSet.contains(cmdStrLower)) {
                m_logger.warn("Enqueuing negative response to " +
                              "non-allowed command \"" + cmd.getCmdString() + "\"" +
                              " (" + cmd.getArgString() + ")");
                actions.appendSyntheticResponse(new FixedSyntheticResponse(500, "Syntax error, command unrecognized"));
                actions.followup();
                return;
            }

            //Check for "EHLO" and "HELO"
            if(cmd.getType() == Command.CommandType.EHLO) {
                m_logger.debug("Enqueuing private response handler to EHLO command so " +
                               "unknown extensions can be disabled");
                actions.sendCommandToServer(cmd, new SessionOpenResponse());
                //Let the handler at least see the EHLO command, for logging and
                //building a "fake" received header for Spamassassin
                m_sessionHandler.observeEHLOCommand(cmd);
                actions.followup();
                return;
            } else if(cmd.getType() == Command.CommandType.HELO) {
                //Let the handler at least see the HELO command, for logging and
                //building a "fake" received header for Spamassassin
                m_sessionHandler.observeEHLOCommand(cmd);
                //fall through and continue like before
            }

            if(m_currentTxHandler != null) {
                if(cmd.getType() == Command.CommandType.RSET) {
                    m_currentTxHandler.handleRSETCommand(cmd, actions);
                    m_currentTxHandler = null;
                }
                else {
                    m_currentTxHandler.handleCommand(cmd, actions);
                }
            }
            else {
                //Odd case, but we're not here to enfore good SMTP (at least not in this class)
                if(cmd.getType() == Command.CommandType.DATA) {
                    TransactionHandler handler = getOrCreateTxHandler();
                    handler.handleCommand(cmd, actions);
                }
                else {
                    m_sessionHandler.handleCommand(cmd, actions);
                }
            }
            actions.followup();
        }

        @Override
        public void handleMAILCommand(TokenResultBuilder resultBuilder,
                                      MAILCommand cmd) {
            TransactionHandler handler = getOrCreateTxHandler();
            SmtpCommandActionsImpl actions = new SmtpCommandActionsImpl(resultBuilder);
            handler.handleMAILCommand(cmd, actions);
            actions.followup();
        }

        @Override
        public void handleRCPTCommand(TokenResultBuilder resultBuilder,
                                      RCPTCommand cmd) {
            TransactionHandler handler = getOrCreateTxHandler();
            SmtpCommandActionsImpl actions = new SmtpCommandActionsImpl(resultBuilder);
            handler.handleRCPTCommand(cmd, actions);
            actions.followup();
        }

        @Override
        public void handleBeginMIME(TokenResultBuilder resultBuilder,
                                    BeginMIMEToken token) {
            TransactionHandler handler = getOrCreateTxHandler();
            SmtpCommandActionsImpl actions = new SmtpCommandActionsImpl(resultBuilder);
            handler.handleBeginMIME(token, actions);
            actions.followup();
        }

        @Override
        public void handleContinuedMIME(TokenResultBuilder resultBuilder,
                                        ContinuedMIMEToken token) {

            TransactionHandler handler = getOrCreateTxHandler();
            if(token.isLast()) {
                m_currentTxHandler = null;
            }
            SmtpCommandActionsImpl actions = new SmtpCommandActionsImpl(resultBuilder);
            handler.handleContinuedMIME(token, actions);
            actions.followup();
        }

        @Override
        public void handleResponse(TokenResultBuilder resultBuilder,
                                   Response resp) {
            m_logger.debug("[handleResponse()] with code " + resp.getCode());
            if(m_outstandingRequests.size() == 0) {
                long timeDiff = System.currentTimeMillis() - getLastServerTimestamp();
                if(timeDiff > LIKELY_TIMEOUT_LENGTH) {
                    m_logger.warn("Unsolicited response from server.  Likely a timeout (" +
                                  timeDiff + " millis since last communication)");
                }
                else {
                    m_logger.info("Response received without a registered handler");
                }
                resultBuilder.addTokenForClient(resp);
                return;
            }
            OutstandingRequest or = m_outstandingRequests.remove(0);
            or.cont.handleResponse(resp, new SmtpResponseActionsImpl(resultBuilder));
            processSynths(or, resultBuilder);
        }

        @Override
        public void handleChunkForClient(TokenResultBuilder resultBuilder,
                                         Chunk chunk) {
            resultBuilder.addTokenForClient(chunk);
        }

        @Override
        public void handleChunkForServer(TokenResultBuilder resultBuilder,
                                         Chunk chunk) {
            resultBuilder.addTokenForServer(chunk);
        }

        @Override
        public void handleCompleteMIME(TokenResultBuilder resultBuilder,
                                       CompleteMIMEToken token) {

            TransactionHandler handler = getOrCreateTxHandler();

            //Looks odd, but the Transaction is complete so just
            //assign the current handler to null.
            m_currentTxHandler = null;

            SmtpCommandActionsImpl actions = new SmtpCommandActionsImpl(resultBuilder);
            handler.handleCompleteMIME(token, actions);
            actions.followup();
        }

        @Override
        public boolean handleServerFIN() {
            boolean ret = m_sessionHandler.handleServerFIN(m_currentTxHandler);
            m_logger.debug("Returning " + ret + " to reciept of server FIN");
            return ret;
        }

        @Override
        public boolean handleClientFIN() {
            boolean ret = m_sessionHandler.handleClientFIN(m_currentTxHandler);
            m_logger.debug("Returning " + ret + " to reciept of client FIN");
            if(ret) {
                //Add extra response handler, just in case
                m_outstandingRequests.add(new OutstandingRequest(
                                                                 new NoopResponseCompletion()));

            }
            return ret;
        }

        @Override
        public void handleFinalized() {
            m_sessionHandler.handleFinalized();
            if(m_currentTxHandler != null) {
                m_currentTxHandler.handleFinalized();
            }
        }

        //============ Inner-Inner Class ==============

        /**
         * Class which handles the ESMTP open response
         */
        private class SessionOpenResponse
            implements ResponseCompletion {
            public void handleResponse(Response resp,
                                       Session.SmtpResponseActions actions) {
                m_logger.debug("Processing response to EHLO Command");
                //Nasty little line below causes
                //the Session to manipulate the response,
                //then lets the session do the same with
                //the already manipulated response.
                actions.sendResponseToClient(
                                             m_sessionHandler.manipulateEHLOResponse(
                                                                                     fixupEHLOResponse(resp)));
            }
        }

    }//ENDOF MySmtpTokenStreamHandler Class Definition

}//ENDOF Session Class Definition
