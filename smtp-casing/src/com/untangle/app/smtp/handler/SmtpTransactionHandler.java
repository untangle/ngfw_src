/**
 * $Id$
 */
package com.untangle.app.smtp.handler;

import java.util.LinkedList;
import java.util.Date;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.app.smtp.BeginMIMEToken;
import com.untangle.app.smtp.Command;
import com.untangle.app.smtp.CommandType;
import com.untangle.app.smtp.CommandWithEmailAddress;
import com.untangle.app.smtp.CompleteMIMEToken;
import com.untangle.app.smtp.ContinuedMIMEToken;
import com.untangle.app.smtp.SmtpMessageEvent;
import com.untangle.app.smtp.Response;
import com.untangle.app.smtp.SmtpTransaction;
import com.untangle.app.smtp.mime.MIMEAccumulator;
import com.untangle.uvm.vnet.Token;

/**
 * SMTP Transaction Handler
 */
public class SmtpTransactionHandler
{
    private enum BufTxState
    {
        INIT,
        GATHER_ENVELOPE,
        BUFFERING_MAIL,
        PASSTHRU_PENDING_NACK,
        DRAIN_MAIL,
        T_B_READING_MAIL,
        DONE
    };

    public enum BlockOrPassResult
    {
        DROP, PASS, TEMPORARILY_REJECT
    };

    private static final Logger logger = Logger.getLogger(SmtpEventHandler.class);

    private static final String RESP_TXT_354 = "Start mail input; end with <CRLF>.<CRLF>";

    private BufTxState state = BufTxState.INIT;

    private MIMEAccumulator accumulator;
    private SmtpMessageEvent messageInfo;
    private MimeMessage msg;
    private boolean isMessageMaster;// Flag indicating if this handler is the "master" of the message (accumulator or
                                    // MIMEMessage). The master is defined as the last app to receive the object in
                                    // question. The master designation is set once a Begin/complete token is received,
                                    // and relinquished once any BEGIN/Complete token is passed
    private final SmtpTransaction tx;

    private int dataResp = -1;// Special case for when we get a negative

    private LinkedList<String> txLog = new LinkedList<>();

    public static final ResponseCompletion PASSTHRU_RESPONSE_COMPLETION = new ResponseCompletion()
    {
        /**
         * Process response.
         * @param session AppTCPSession to process.
         * @param resp    Response to process.
         */
        @Override
        public void handleResponse( AppTCPSession session, Response resp )
        {
            logger.debug("Sending response " + resp.getCode() + " to client");
            session.sendObjectToClient( resp );
        }
    };

    public static final ResponseCompletion NOOP_RESPONSE_COMPLETION = new ResponseCompletion()
    {
        /**
         * Process response.
         * @param session AppTCPSession to process.
         * @param resp    Response to process.
         */
        @Override
        public void handleResponse( AppTCPSession session, Response resp )
        {

        }
    };

    // response to our sending of a "real" DATA
    // command to the server. In this special
    // case, we need to queue what the server said
    // to the DATA command and instead send it
    // as a result of the client's <CRLF>.<CRLF>

    /**
     * Initialize instance of SmtpTransactionHandler.
     * @param tx SmtpTransaction instance of SmtpTransaction.
     */
    SmtpTransactionHandler(SmtpTransaction tx) {
        this.tx = tx;
        addToTxLog("---- Initial state " + state + " (" + new Date() + ") -------");
        isMessageMaster = false;
    }

    /**
     * Return transaction.
     * @return SmtpTransaction.
     */
    public final SmtpTransaction getTransaction()
    {
        return tx;
    }

    /**
     * Process RSET command.
     * @param session      AppTCPSession to process.
     * @param command      Command to process.
     * @param stateMachine SmtpEventHandler to process.
     */
    public void handleRSETCommand( AppTCPSession session, Command command, SmtpEventHandler stateMachine )
    {

        logReceivedToken(command);

        // Look for the state we understand. Note I included "INIT"
        // but that should be impossible by definition
        if (state == BufTxState.GATHER_ENVELOPE || state == BufTxState.INIT) {
            addToTxLog("Aborting at client request");
            stateMachine.transactionEnded( session, this );
            getTransaction().reset();
            stateMachine.sendCommandToServer( session, command, PASSTHRU_RESPONSE_COMPLETION );
            changeState(BufTxState.DONE);
            closeMessageResources(false);
        } else {// State/command misalignment
            addToTxLog("Impossible command now: \"" + command + "\"");
            logger.error("Impossible command now: \"" + command + "\"");
            dumpToLogger(Level.ERROR);
            stateMachine.transactionEnded( session, this );
            stateMachine.sendCommandToServer( session, command, PASSTHRU_RESPONSE_COMPLETION );
            changeState(BufTxState.DONE);
        }
    }

    /**
     * Process command.
     * @param session      AppTCPSession to process.
     * @param command      Command to process.
     * @param stateMachine SmtpEventHandler to process.
     * @param immediateActions List of Response.
     */
    public void handleCommand( AppTCPSession session, Command command, SmtpEventHandler stateMachine, List<Response> immediateActions )
    {
        logReceivedToken(command);

        if (state == BufTxState.GATHER_ENVELOPE || state == BufTxState.INIT) {
            if (command.getType() == CommandType.DATA) {
                // Don't passthru
                addToTxLog("Enqueue synthetic 354 for client");
                stateMachine.appendSyntheticResponse( session, new Response(354, RESP_TXT_354), immediateActions);
                changeState(BufTxState.BUFFERING_MAIL);
            } else {
                addToTxLog("Passthru to client");
                stateMachine.sendCommandToServer( session, command, PASSTHRU_RESPONSE_COMPLETION );
            }
        } else {
            addToTxLog("Impossible command now: \"" + command + "\"");
            logger.error("Impossible command now: \"" + command + "\"");
            dumpToLogger(Level.ERROR);
            stateMachine.sendCommandToServer( session, command, PASSTHRU_RESPONSE_COMPLETION );
            stateMachine.transactionEnded( session, this );
            changeState(BufTxState.DONE);
        }
    }

    /**
     * Process MAIL or RCPT command.
     * @param session      AppTCPSession to process.
     * @param command      Command to process.
     * @param stateMachine SmtpEventHandler to process.
     * @param compl         ResponseCompletion to process.
     */
    private void handleMAILOrRCPTCommand( AppTCPSession session, Command command, SmtpEventHandler stateMachine, ResponseCompletion compl )
    {

        logReceivedToken(command);

        if (state == BufTxState.GATHER_ENVELOPE || state == BufTxState.INIT) {
            if (state == BufTxState.INIT) {
                changeState(BufTxState.GATHER_ENVELOPE);
            }
            addToTxLog("Pass " + command.getType() + " command to server, register callback to modify envelope at response");
            stateMachine.sendCommandToServer( session, command, compl );
        } else {
            addToTxLog("Impossible command now: \"" + command + "\"");
            logger.error("Impossible command now: \"" + command + "\"");
            dumpToLogger(Level.ERROR);
            stateMachine.sendCommandToServer( session, command, PASSTHRU_RESPONSE_COMPLETION );
            stateMachine.transactionEnded( session, this );
            changeState(BufTxState.DONE);
        }
    }

    /**
     * Process MAIL command.
     * @param session      AppTCPSession to process.
     * @param command      CommandWithEmailAddress to process.
     * @param stateMachine SmtpEventHandler to process.
     */
    public void handleMAILCommand( AppTCPSession session, final CommandWithEmailAddress command, SmtpEventHandler stateMachine )
    {
        getTransaction().fromRequest(command.getAddress());
        handleMAILOrRCPTCommand( session, command, stateMachine, new ResponseCompletion()
        {
            /**
             * Process response.
             * @param session AppTCPSession to process.
             * @param resp    Response to process.
            */
            @Override
            public void handleResponse( AppTCPSession session, Response resp )
            {
                getTransaction().fromResponse(command.getAddress(), (resp.getCode() < 300));
                logger.debug("Sending response " + resp.getCode() + " to client");
                session.sendObjectToClient( resp );
            }
        });
    }

    /**
     * Process RCPT command.
     * @param session      AppTCPSession to process.
     * @param command      CommandWithEmailAddress to process.
     * @param stateMachine SmtpEventHandler to process.
     */
    public void handleRCPTCommand( AppTCPSession session, final CommandWithEmailAddress command, SmtpEventHandler stateMachine )
    {
        getTransaction().toRequest(command.getAddress());
        handleMAILOrRCPTCommand( session, command, stateMachine, new ResponseCompletion()
        {
            /**
             * Process response.
             * @param session AppTCPSession to process.
             * @param resp    Response to process.
             */
            @Override
            public void handleResponse( AppTCPSession session, Response resp )
            {
                getTransaction().toResponse(command.getAddress(), (resp.getCode() < 300));
                logger.debug("Sending response " + resp.getCode() + " to client");
                session.sendObjectToClient( resp );
            }
        });
    }

    /**
     * Process MIME start.
     * @param session      AppTCPSession to process.
     * @param token BeginMIMEToken to process.
     * @param stateMachine SmtpEventHandler to process.
     * @param immediateActions List of Response to process.
     */
    public void handleBeginMIME( AppTCPSession session, BeginMIMEToken token, SmtpEventHandler stateMachine, List<Response> immediateActions )
    {
        logReceivedToken(token);

        accumulator = token.getMIMEAccumulator();
        messageInfo = token.getSmtpMessageEvent();
        isMessageMaster = true;

        handleMIMEChunkToken( session, true, false, null, stateMachine, immediateActions );
    }

    /**
     * Process MIME start.
     * @param session      AppTCPSession to process.
     * @param token ContinuedMIMEToken to process.
     * @param stateMachine SmtpEventHandler to process.
     * @param immediateActions List of Response to process.
     */
    public void handleContinuedMIME( AppTCPSession session, ContinuedMIMEToken token, SmtpEventHandler stateMachine, List<Response> immediateActions )
    {
        logReceivedToken(token);

        handleMIMEChunkToken( session, false, token.isLast(), token, stateMachine, immediateActions );
    }

    /**
     * Process MIME start.
     * @param session      AppTCPSession to process.
     * @param token CompleteMIMEToken to process.
     * @param stateMachine SmtpEventHandler to process.
     * @param immediateActions List of Response to process.
     */
    public void handleCompleteMIME( AppTCPSession session, CompleteMIMEToken token, SmtpEventHandler stateMachine, List<Response> immediateActions )
    {

        logReceivedToken(token);

        msg = token.getMessage();
        messageInfo = token.getSmtpMessageEvent();
        isMessageMaster = true;
        accumulator = null;

        handleMIMEChunkToken( session, true, true, token, stateMachine, immediateActions );

    }

    /**
     * Process finalized.
     */
    public void handleFinalized()
    {
        closeMessageResources(true);
    }

    /**
     * Remove temp files.
     * @param token Token to reference temp file.
     */
    private void cleanupTempFile(Token token)
    {
        if (accumulator != null) {
            accumulator.dispose();
            accumulator = null;
        } else {
            //this handler is not the owner of the accumulator, but received a CompleteMIMEToken from the owner
            if (token != null && token instanceof CompleteMIMEToken) {
                ((CompleteMIMEToken) token).cleanupTempFile();
            }
        }
    }

    /**
     * Process MIME chunk token.
     * @param session      AppTCPSession to process.
     * @param isFirst if true first chunk, false if not.
     * @param isLast if true last chunk, false if not.
     * @param token CompleteMIMEToken to process.
     * @param stateMachine SmtpEventHandler to process.
     * @param immediateActions List of Response to process.
     */
    private void handleMIMEChunkToken( final AppTCPSession session,
                                  boolean isFirst, boolean isLast,
                                  Token token,
                                  final SmtpEventHandler stateMachine,
                                  List<Response> immediateActions )
    {
        ContinuedMIMEToken continuedToken = null;
        if (token != null && token instanceof ContinuedMIMEToken) {
            continuedToken = (ContinuedMIMEToken) token;
        }
        switch (state) {
            case INIT:
            case GATHER_ENVELOPE:
            case DONE:
                addToTxLog("Impossible command now: MIME chunk (first? " + isFirst + ", isLast? " + isLast);
                logger.error("Impossible command now: MIME chunk (first? " + isFirst + ", isLast? " + isLast);
                dumpToLogger(Level.ERROR);
                appendChunkToken(continuedToken);
                changeState(BufTxState.DONE);
                break;
            case BUFFERING_MAIL:
                // ------Page 21--------
                if (isLast) {
                    // We have the complete message.
                    addToTxLog("Have whole message.  Evaluate");
                    if (continuedToken != null) {
                        appendChunkToken(continuedToken);
                    }

                    BlockOrPassResult action = evaluateMessage( session, true, stateMachine );
                    switch (action) {
                        case PASS:
                            addToTxLog("Message passed evaluation");
                            // We're passing the message (or there was a silent parser error)

                            // Disable client tokens while we "catch-up" by issuing the DATA command
                            stateMachine.disableClientTokens( session );

                            addToTxLog("Send synthetic DATA to server, continue when reply arrives");
                            stateMachine.sendCommandToServer( session, new Command(CommandType.DATA), new ResponseCompletion()
                            {
                                /**
                                  * Process response.
                                  * @param session AppTCPSession to process.
                                  * @param resp    Response to process.
                                  */
                                @Override
                                public void handleResponse( AppTCPSession session, Response resp )
                                {
                                    handleResponseAfterPassedMessage( session, stateMachine, resp );
                                }
                            });

                            changeState(BufTxState.DONE);
                            break;
                        case DROP:
                            addToTxLog("Message failed evaluation (we're going to block it)");
                            // Send a fake 250 to client
                            addToTxLog("Enqueue synthetic 250 to client");
                            stateMachine.appendSyntheticResponse( session, new Response(250, "OK"), immediateActions);

                            // Send REST to server (ignore result)
                            addToTxLog("Send synthetic RSET to server (ignoring the response)");
                            stateMachine.sendCommandToServer( session, new Command(CommandType.RSET), NOOP_RESPONSE_COMPLETION );

                            changeState(BufTxState.DONE);
                            stateMachine.transactionEnded( session, this );
                            getTransaction().reset();
                            cleanupTempFile(token);
                            break;
                        case TEMPORARILY_REJECT:
                            // We're blocking the message
                            addToTxLog("Message temporarily rejected");
                            // Send a fake 451 to client
                            addToTxLog("Enqueue synthetic 451 to client");
                            stateMachine.appendSyntheticResponse( session, new Response(451, "Please try again later"), immediateActions);

                            // Send REST to server (ignore result)
                            addToTxLog("Send synthetic RSET to server (ignoring the response)");
                            stateMachine.sendCommandToServer( session, new Command(CommandType.RSET), NOOP_RESPONSE_COMPLETION );

                            changeState(BufTxState.DONE);
                            stateMachine.transactionEnded( session, this );
                            getTransaction().reset();
                            cleanupTempFile(token);
                            break;
                        default:
                            logger.warn("unhandled action: " + action);
                            break;
                    }
                } else {
                    addToTxLog("Not last MIME chunk, append to the file");
                    appendChunkToken(continuedToken);
                    // We go down one of three branches from here. We begin passthru if the
                    // mail is too large or if we've timed out.
                    //
                    // We Trickle&Buffer if we've timed out but the subclass wants to keep going.
                    //
                    // We simply record the new chunk if nothing else is to be done.
                    boolean timedOut = stateMachine.shouldBeginTrickle( session );

                    if (timedOut && !stateMachine.isBufferAndTrickle( session )) {
                        // Passthru DATA Sent
                        addToTxLog("Mail timed-out w/o needing to buffer (trickle, not buffer-and-trickle)");
                        // Disable tokens from client until we get the disposition to the DATA command
                        stateMachine.disableClientTokens( session );
                        // Send the DATA command to the server
                        addToTxLog("Send synthetic DATA to server, continue when response arrives");
                        // Use the contnuation shared by this and the T_B_READING_MAIL state. The
                        // continuations are 99% the same, except they differ in their next state upon success
                        stateMachine.sendCommandToServer( session, new Command(CommandType.DATA), new ResponseCompletion()
                        {
                            /**
                              * Process response.
                              * @param session AppTCPSession to process.
                              * @param resp    Response to process.
                              */
                            @Override
                            public void handleResponse( AppTCPSession session, Response resp)
                            {
                                handleMsgIncompleteDataContinuation( session, resp, stateMachine, BufTxState.DRAIN_MAIL );
                            }
                        });
                        changeState(BufTxState.DONE);
                    } else if (timedOut && stateMachine.isBufferAndTrickle( session )) {
                        // T&B DATA Sent
                        addToTxLog("Mail timed out.  Begin trickle and buffer");
                        // Disable client until we can hear back from the "DATA" command
                        stateMachine.disableClientTokens( session );
                        // Send the DATA command to the server and set-up the callback
                        addToTxLog("Send synthetic DATA to server, continue when response arrives");
                        stateMachine.sendCommandToServer( session, new Command(CommandType.DATA), new ResponseCompletion()
                        {
                            /**
                              * Process response.
                              * @param session AppTCPSession to process.
                              * @param resp    Response to process.
                              */
                            @Override
                            public void handleResponse( AppTCPSession session, Response resp )
                            {
                                handleMsgIncompleteDataContinuation( session, resp, stateMachine, BufTxState.T_B_READING_MAIL );
                            }
                        });
                        changeState(BufTxState.DONE);
                    } else {
                        // The chunk is already recorded. Nothing to do. No delta state
                    }
                }// ENDOF Not Last Token

                break;
            case DRAIN_MAIL:
                // Page 25
                addToTxLog("Pass this chunk on to the server");
                if (isLast) {
                    // Make sure we're no longer the active transaction
                    stateMachine.transactionEnded( session, this );

                    // Install the simple callback handler (doesn't do much)
                    stateMachine.sendFinalMIMEToServer( session, continuedToken, new ResponseCompletion()
                    {
                        /**
                          * Process response.
                          * @param session AppTCPSession to process.
                          * @param resp    Response to process.
                          */
                        @Override
                        public void handleResponse( AppTCPSession session, Response resp)
                        {
                            handleMailTransmissionContinuation( session, resp, stateMachine );
                        }
                    });

                    changeState(BufTxState.DONE);
                } else {
                    // No change in state, and the chunk has already been passed along.
                    stateMachine.sendContinuedMIMEToServer( session, continuedToken);
                }
                break;
            case PASSTHRU_PENDING_NACK:
                // Page 27
                // Let the token fall on the floor, as it'll never be accepted by the server.
                // We're just combing through the junk from the client getting to the point where we can NACK
                if (isLast) {
                    // Make sure we're no longer the active transaction
                    stateMachine.transactionEnded( session, this );
                    // Transaction Failed
                    getTransaction().failed();
                    // Pass along same error (whatever it was) to client
                    addToTxLog("End of queued NACK.  Send " + dataResp + " to client");
                    stateMachine.appendSyntheticResponse( session, new Response(dataResp, ""), immediateActions);
                    // We're done
                    changeState(BufTxState.DONE);
                } else {
                    // Nothing interesting to do.
                }
                break;
            case T_B_READING_MAIL:
                // Page 29
                // Write it to the file regardless
                appendChunkToken(continuedToken);
                if (isLast) {
                    addToTxLog("Trickle and buffer.  Whole message obtained.  Evaluate");
                    BlockOrPassResult action = evaluateMessage( session, false, stateMachine );

                    switch (action) {
                        case PASS:
                            addToTxLog("Evaluation passed");
                            stateMachine.sendFinalMIMEToServer( session, continuedToken, new ResponseCompletion()
                            {
                                /**
                                 * Process response.
                                 * @param session AppTCPSession to process.
                                 * @param resp    Response to process.
                                 */
                                @Override
                                public void handleResponse( AppTCPSession session, Response resp)
                                {
                                    handleMailTransmissionContinuation( session, resp, stateMachine );
                                }
                            });
                            break;
                        case DROP:
                            // Block, the hard way...
                            addToTxLog("Evaluation failed.  Send a \"fake\" 250 to client then shutdown server");
                            stateMachine.appendSyntheticResponse( session, new Response(250, "OK"), immediateActions);
                            stateMachine.transactionEnded( session, this );
                            // Put a Response handler here, in case the goofy server
                            // sends an ACK to the FIN (which Exim seems to do!?!)
                            stateMachine.sendFINToServer( session, NOOP_RESPONSE_COMPLETION );
                            addToTxLog("Replace SessionHandler with shutdown dummy");
                            stateMachine.startShutingDown( session );

                            break;
                        case TEMPORARILY_REJECT:
                            // Block, the hard way...
                            addToTxLog("Evaluation failed.  Send a \"fake\" 451 to client then shutdown server");
                            stateMachine.appendSyntheticResponse( session, new Response(451, "Please try again later"), immediateActions);
                            stateMachine.transactionEnded( session, this );
                            // Put a Response handler here, in case the goofy server
                            // sends an ACK to the FIN (which Exim seems to do!?!)
                            stateMachine.sendFINToServer( session, NOOP_RESPONSE_COMPLETION );
                            addToTxLog("Replace SessionHandler with shutdown dummy");
                            stateMachine.startShutingDown( session );

                            break;
                        default:
                            logger.warn("unhandled action: " + action);
                            break;
                    }
                } else {
                    stateMachine.sendContinuedMIMEToServer( session, continuedToken );
                }
                break;
            default:
                addToTxLog("Error - Unknown State " + state);
                changeState(BufTxState.DONE);
                stateMachine.transactionEnded( session, this );
                appendChunkToken(continuedToken);
                if (isLast) {
                    stateMachine.sendContinuedMIMEToServer( session, continuedToken );
                } else {
                    stateMachine.sendFinalMIMEToServer( session, continuedToken, PASSTHRU_RESPONSE_COMPLETION );
                }
        }
    }

    /**
     * action taken on the response callback, after evaluating a message as PASS
     * 
     * @param session      AppTCPSession to process.
     * @param stateMachine SmtpEventHandler to process.
     * @param resp Response to process.
     */
    private void handleResponseAfterPassedMessage( AppTCPSession session, final SmtpEventHandler stateMachine, Response resp )
    {
        logReceivedResponse(resp);
        addToTxLog("Response to DATA command was " + resp.getCode());

        // Save this in a variable, so we can pass it along later (if not positive)
        dataResp = resp.getCode();

        stateMachine.enableClientTokens( session );
        stateMachine.transactionEnded( session, SmtpTransactionHandler.this );

        if (resp.getCode() < 400) {
            // Don't forward to client

            // Pass either complete MIME or begin/end (if there was a parse error)
            if (msg == null) {
                addToTxLog("Passing along an unparsable MIME message in two tokens");
                isMessageMaster = false;
                stateMachine.sendBeginMIMEToServer( session, new BeginMIMEToken(accumulator, messageInfo) );
                stateMachine.sendFinalMIMEToServer( session, new ContinuedMIMEToken(accumulator.createChunkToken(null, true)),
                        new ResponseCompletion()
                        {
                            /**
                              * Process response.
                              * @param session AppTCPSession to process.
                              * @param resp    Response to process.
                              */
                            @Override
                            public void handleResponse( AppTCPSession session, Response resp )
                            {
                                handleMailTransmissionContinuation( session, resp, stateMachine );
                            }
                        });
                accumulator = null;
            } else {
                addToTxLog("Passing along parsed MIME in one token");
                if (accumulator != null) {
                    accumulator.closeInput();
                    accumulator = null;
                }
                stateMachine.sentWholeMIMEToServer( session, new CompleteMIMEToken(msg, messageInfo),
                        new ResponseCompletion()
                        {
                            /**
                              * Process response.
                              * @param session AppTCPSession to process.
                              * @param resp    Response to process.
                              */
                            @Override
                            public void handleResponse( AppTCPSession session, Response resp )
                            {
                                handleMailTransmissionContinuation( session, resp, stateMachine );
                            }
                        });
                isMessageMaster = false;
                msg = null;
            }
            changeState(BufTxState.DONE);
        } else {
            // Discard message
            closeMessageResources(false);

            // Transaction Failed
            getTransaction().failed();

            // Pass along same error (whatever it was) to client
            stateMachine.sendResponseToClient( session, resp );

            // We're done
            changeState(BufTxState.DONE);
        }
    }

    /**
     * Append chunk token.
     * If null, just ignore
     * @param continuedToken ContinuedMIMEToken to process.
     */
    private void appendChunkToken(ContinuedMIMEToken continuedToken)
    {
        if (continuedToken == null) {
            return;
        }
        if (accumulator == null) {
            logger.error("Received ContinuedMIMEToken without a MIMEAccumulator set");
            return;
        }
        if (!accumulator.appendChunkTokenToFile(continuedToken.getMIMEChunkToken())) {
            logger.error("Error appending MIME ChunkToken");
        }

    }

    /**
     * Change to new State.
     * @param newState BufTxState to change to.
     */
    private void changeState(BufTxState newState)
    {
        logRecordStateChange(state, newState);
        state = newState;
    }

    /**
     * Close message resources.
     * @param force If true foricbly do it, false otherwise.
     */
    private void closeMessageResources(boolean force)
    {
        if (isMessageMaster || force) {
            if (accumulator != null) {
                accumulator.dispose();
                accumulator = null;
            }
            if (msg != null) {
                msg = null;
            }
        }
    }

    /**
     * Returns PASS if we should pass. If there is a parsing error, this also returns PASS. If the message was changed,
     * it'll just be picked-up implicitly.
     * @param session      AppTCPSession to process.
     * @param canModify If true modify allowed, otherwise not.
     * @param stateMachine SmtpEventHandler to process.
     * @return BlockOrPassResult result.
     */
    private BlockOrPassResult evaluateMessage( AppTCPSession session, boolean canModify, SmtpEventHandler stateMachine)
    {
        if (msg == null) {
            msg = accumulator.parseBody(messageInfo);
            accumulator.closeInput();
            if (msg == null) {
                addToTxLog("Parse error on MIME.  Assume it passed scanning");
                return BlockOrPassResult.PASS;
            }
        }
        if (canModify) {
            ScannedMessageResult result = stateMachine.blockPassOrModify( session, msg, getTransaction(), messageInfo );
            if (result.messageModified()) {
                addToTxLog("Evaluation modified MIME message");
                msg = result.getMessage();
                return BlockOrPassResult.PASS;
            } else {
                return result.getAction();
            }
        } else {
            return stateMachine.blockOrPass( session, msg, getTransaction(), messageInfo );
        }
    }

    /**
     * Handle mail transmisison continuation.
     * @param session      AppTCPSession to process.
     * @param resp      Response.
     * @param stateMachine SmtpEventHandler to process.
     */
    public void handleMailTransmissionContinuation( AppTCPSession session, Response resp, SmtpEventHandler stateMachine )
    {
        logReceivedResponse(resp);
        addToTxLog("Response to mail transmission command was " + resp.getCode());

        if (resp.getCode() < 300) {
            getTransaction().commit();
        } else {
            getTransaction().failed();
        }
        changeState(BufTxState.DONE);
        stateMachine.transactionEnded( session, SmtpTransactionHandler.this );
        logger.debug("Sending response " + resp.getCode() + " to client");
        session.sendObjectToClient( resp );
    }

    /**
     * Handle incomplete data transmisison continuation.
     * @param session      AppTCPSession to process.
     * @param resp      Response.
     * @param stateMachine SmtpEventHandler to process.
     * @param nextStateIfPositive BufTxState to process.
     */
    public void handleMsgIncompleteDataContinuation( AppTCPSession session, Response resp, SmtpEventHandler stateMachine, BufTxState nextStateIfPositive )
    {

        logReceivedResponse(resp);
        addToTxLog("Response to DATA command was " + resp.getCode());

        // Save this in a variable, so we can pass it along later (if not positive)
        dataResp = resp.getCode();

        stateMachine.enableClientTokens( session );

        if (resp.getCode() < 400) {
            addToTxLog("Begin trickle with BeginMIMEToken");
            stateMachine.sendBeginMIMEToServer( session, new BeginMIMEToken(accumulator, messageInfo) );
            isMessageMaster = false;
            changeState(nextStateIfPositive);
        } else {
            // Discard message
            closeMessageResources(false);

            stateMachine.sendCommandToServer( session, new Command(CommandType.RSET), NOOP_RESPONSE_COMPLETION );
            changeState(BufTxState.PASSTHRU_PENDING_NACK);
        }
    }

    // ============== Logging ==========================

    /**
     * Log received token.
     * @param token Token to log.
     */
    void logReceivedToken(Token token)
    {
        addToTxLog("----Received Token " + token + "--------");
    }

    /**
     * Log received response.
     * @param resp Response to log.
     */
    void logReceivedResponse(Response resp)
    {
        addToTxLog("----Received Response " + resp.getCode() + "--------");
    }

    /**
     * Log record state change.
     * @param old Old BufTxState
     * @param newState New BufTxState.
     */
    void logRecordStateChange(BufTxState old, BufTxState newState)
    {
        addToTxLog("----Change State " + old + "->" + newState + "-----------");
    }

    /**
     * Add message to transmit log.
     * @param str String to add.
     */
    void addToTxLog( String str )
    {
        if (logger.isDebugEnabled())
            logger.debug("addToTxLog( " + System.identityHashCode(txLog) + " ): " + str );
        txLog.add( str );
    }
    
    /**
     * Dump to logger.
     * @param level Level to dump.
     */
    @SuppressWarnings("unchecked")
    void dumpToLogger(Level level)
    {
        logger.log(level, "=======BEGIN Transaction Log============= txLog:" + System.identityHashCode(txLog));
        for ( String s : (List<String>) txLog.clone() ) {
            logger.log(level, s);
        }
        logger.log(level, "=======ENDOF Transaction Log=============");
        txLog.clear();
    }
}
