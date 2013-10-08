package com.untangle.node.smtp.handler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.untangle.node.smtp.BeginMIMEToken;
import com.untangle.node.smtp.Command;
import com.untangle.node.smtp.CommandType;
import com.untangle.node.smtp.CommandWithEmailAddress;
import com.untangle.node.smtp.CompleteMIMEToken;
import com.untangle.node.smtp.ContinuedMIMEToken;
import com.untangle.node.smtp.MessageInfo;
import com.untangle.node.smtp.Response;
import com.untangle.node.smtp.SmtpTransaction;
import com.untangle.node.smtp.mime.MIMEAccumulator;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenResultBuilder;

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

    private static final Logger logger = Logger.getLogger(SmtpStateMachine.class);

    private static final String RESP_TXT_354 = "Start mail input; end with <CRLF>.<CRLF>";

    private BufTxState state = BufTxState.INIT;

    private MIMEAccumulator accumulator;
    private MessageInfo messageInfo;
    private MimeMessage msg;
    private boolean isMessageMaster;// Flag indicating if this handler is the "master" of the message (accumulator or
                                    // MIMEMessage). The master is defined as the last node to receive the object in
                                    // question. The master designation is set once a Begin/complete token is received,
                                    // and relinquished once any BEGIN/Complete token is passed
    private final SmtpTransaction tx;

    private int dataResp = -1;// Special case for when we get a negative

    private List<String> txLog = new ArrayList<String>();

    public static final ResponseCompletion PASSTHRU_RESPONSE_COMPLETION = new ResponseCompletion()
    {
        @Override
        public void handleResponse(Response resp, TokenResultBuilder ts)
        {
            logger.debug("Sending response " + resp.getCode() + " to client");
            ts.addTokenForClient(resp);
        }
    };

    public static final ResponseCompletion NOOP_RESPONSE_COMPLETION = new ResponseCompletion()
    {
        @Override
        public void handleResponse(Response resp, TokenResultBuilder ts)
        {

        }
    };

    // response to our sending of a "real" DATA
    // command to the server. In this special
    // case, we need to queue what the server said
    // to the DATA command and instead send it
    // as a result of the client's <CRLF>.<CRLF>

    SmtpTransactionHandler(SmtpTransaction tx) {
        this.tx = tx;
        txLog.add("---- Initial state " + state + " (" + new Date() + ") -------");
        isMessageMaster = false;
    }

    public final SmtpTransaction getTransaction()
    {
        return tx;
    }

    public void handleRSETCommand(Command command, SmtpStateMachine stateMachine, TokenResultBuilder ts)
    {

        logReceivedToken(command);

        // Look for the state we understand. Note I included "INIT"
        // but that should be impossible by definition
        if (state == BufTxState.GATHER_ENVELOPE || state == BufTxState.INIT) {
            txLog.add("Aborting at client request");
            stateMachine.transactionEnded(this);
            getTransaction().reset();
            stateMachine.sendCommandToServer(command, PASSTHRU_RESPONSE_COMPLETION, ts);
            changeState(BufTxState.DONE);
            closeMessageResources(false);
        } else {// State/command misalignment
            txLog.add("Impossible command now: \"" + command + "\"");
            dumpToLogger(Level.ERROR);
            stateMachine.transactionEnded(this);
            stateMachine.sendCommandToServer(command, PASSTHRU_RESPONSE_COMPLETION, ts);
            changeState(BufTxState.DONE);
        }
    }

    public void handleCommand(Command command, SmtpStateMachine stateMachine, TokenResultBuilder ts,
            List<Response> immediateActions)
    {
        logReceivedToken(command);

        if (state == BufTxState.GATHER_ENVELOPE || state == BufTxState.INIT) {
            if (command.getType() == CommandType.DATA) {
                // Don't passthru
                txLog.add("Enqueue synthetic 354 for client");
                stateMachine.appendSyntheticResponse(new Response(354, RESP_TXT_354), immediateActions);
                changeState(BufTxState.BUFFERING_MAIL);
            } else {
                txLog.add("Passthru to client");
                stateMachine.sendCommandToServer(command, PASSTHRU_RESPONSE_COMPLETION, ts);
            }
        } else {
            txLog.add("Impossible command now: \"" + command + "\"");
            dumpToLogger(Level.ERROR);
            stateMachine.sendCommandToServer(command, PASSTHRU_RESPONSE_COMPLETION, ts);
            stateMachine.transactionEnded(this);
            changeState(BufTxState.DONE);
        }
    }

    private void handleMAILOrRCPTCommand(Command command, SmtpStateMachine stateMachine, TokenResultBuilder ts,
            ResponseCompletion compl)
    {

        logReceivedToken(command);

        if (state == BufTxState.GATHER_ENVELOPE || state == BufTxState.INIT) {
            if (state == BufTxState.INIT) {
                changeState(BufTxState.GATHER_ENVELOPE);
            }
            txLog.add("Pass " + command.getType()
                    + " command to server, register callback to modify envelope at response");
            stateMachine.sendCommandToServer(command, compl, ts);
        } else {
            txLog.add("Impossible command now: \"" + command + "\"");
            dumpToLogger(Level.ERROR);
            stateMachine.sendCommandToServer(command, PASSTHRU_RESPONSE_COMPLETION, ts);
            stateMachine.transactionEnded(this);
            changeState(BufTxState.DONE);
        }
    }

    public void handleMAILCommand(final CommandWithEmailAddress command, SmtpStateMachine stateMachine,
            TokenResultBuilder ts)
    {
        getTransaction().fromRequest(command.getAddress());
        handleMAILOrRCPTCommand(command, stateMachine, ts, new ResponseCompletion()
        {
            @Override
            public void handleResponse(Response resp, TokenResultBuilder ts)
            {
                getTransaction().fromResponse(command.getAddress(), (resp.getCode() < 300));
                logger.debug("Sending response " + resp.getCode() + " to client");
                ts.addTokenForClient(resp);
            }
        });
    }

    public void handleRCPTCommand(final CommandWithEmailAddress command, SmtpStateMachine stateMachine,
            TokenResultBuilder ts)
    {
        getTransaction().toRequest(command.getAddress());
        handleMAILOrRCPTCommand(command, stateMachine, ts, new ResponseCompletion()
        {
            @Override
            public void handleResponse(Response resp, TokenResultBuilder ts)
            {
                getTransaction().toResponse(command.getAddress(), (resp.getCode() < 300));
                logger.debug("Sending response " + resp.getCode() + " to client");
                ts.addTokenForClient(resp);
            }
        });
    }

    public void handleBeginMIME(BeginMIMEToken token, SmtpStateMachine stateMachine, List<Response> immediateActions,
            TokenResultBuilder ts)
    {

        logReceivedToken(token);

        accumulator = token.getMIMEAccumulator();
        messageInfo = token.getMessageInfo();
        isMessageMaster = true;

        handleMIMEChunk(true, false, null, stateMachine, immediateActions, ts);
    }

    public void handleContinuedMIME(ContinuedMIMEToken token, SmtpStateMachine stateMachine,
            List<Response> immediateActions, TokenResultBuilder ts)
    {
        logReceivedToken(token);

        handleMIMEChunk(false, token.isLast(), token, stateMachine, immediateActions, ts);
    }

    public void handleCompleteMIME(CompleteMIMEToken token, SmtpStateMachine stateMachine,
            List<Response> immediateActions, TokenResultBuilder ts)
    {

        logReceivedToken(token);

        msg = token.getMessage();
        messageInfo = token.getMessageInfo();
        isMessageMaster = true;
        accumulator = null;

        handleMIMEChunk(true, true, token, stateMachine, immediateActions, ts);

    }

    public void handleFinalized()
    {
        closeMessageResources(true);
    }

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

    private void handleMIMEChunk(boolean isFirst, boolean isLast, Token token, final SmtpStateMachine stateMachine,
            List<Response> immediateActions, final TokenResultBuilder ts)
    {
        ContinuedMIMEToken continuedToken = null;
        if (token != null && token instanceof ContinuedMIMEToken) {
            continuedToken = (ContinuedMIMEToken) token;
        }
        switch (state) {
            case INIT:
            case GATHER_ENVELOPE:
            case DONE:
                txLog.add("Impossible command now: MIME chunk (first? " + isFirst + ", isLast? " + isLast);
                dumpToLogger(Level.ERROR);
                appendChunk(continuedToken);
                changeState(BufTxState.DONE);
                break;
            case BUFFERING_MAIL:
                // ------Page 21--------
                if (isLast) {
                    // We have the complete message.
                    txLog.add("Have whole message.  Evaluate");
                    if (continuedToken != null) {
                        appendChunk(continuedToken);
                    }

                    BlockOrPassResult action = evaluateMessage(true, stateMachine);
                    switch (action) {
                        case PASS:
                            txLog.add("Message passed evaluation");
                            // We're passing the message (or there was a silent parser error)

                            // Disable client tokens while we "catch-up" by issuing the DATA command
                            stateMachine.disableClientTokens();

                            txLog.add("Send synthetic DATA to server, continue when reply arrives");
                            stateMachine.sendCommandToServer(new Command(CommandType.DATA), new ResponseCompletion()
                            {
                                @Override
                                public void handleResponse(Response resp, TokenResultBuilder ts)
                                {
                                    handleResponseAfterPassedMessage(stateMachine, resp, ts);
                                }
                            }, ts);

                            changeState(BufTxState.DONE);
                            break;
                        case DROP:
                            txLog.add("Message failed evaluation (we're going to block it)");
                            // Send a fake 250 to client
                            txLog.add("Enqueue synthetic 250 to client");
                            stateMachine.appendSyntheticResponse(new Response(250, "OK"), immediateActions);

                            // Send REST to server (ignore result)
                            txLog.add("Send synthetic RSET to server (ignoring the response)");
                            stateMachine.sendCommandToServer(new Command(CommandType.RSET), NOOP_RESPONSE_COMPLETION,
                                    ts);

                            changeState(BufTxState.DONE);
                            stateMachine.transactionEnded(this);
                            getTransaction().reset();
                            cleanupTempFile(token);
                            break;
                        case TEMPORARILY_REJECT:
                            // We're blocking the message
                            txLog.add("Message temporarily rejected");
                            // Send a fake 451 to client
                            txLog.add("Enqueue synthetic 451 to client");
                            stateMachine.appendSyntheticResponse(new Response(451, "Please try again later"),
                                    immediateActions);

                            // Send REST to server (ignore result)
                            txLog.add("Send synthetic RSET to server (ignoring the response)");
                            stateMachine.sendCommandToServer(new Command(CommandType.RSET), NOOP_RESPONSE_COMPLETION,
                                    ts);

                            changeState(BufTxState.DONE);
                            stateMachine.transactionEnded(this);
                            getTransaction().reset();
                            cleanupTempFile(token);
                            break;
                        default:
                            logger.warn("unhandled action: " + action);
                            break;
                    }
                } else {
                    txLog.add("Not last MIME chunk, append to the file");
                    appendChunk(continuedToken);
                    // We go down one of three branches from here. We begin passthru if the
                    // mail is too large or if we've timed out.
                    //
                    // We Trickle&Buffer if we've timed out but the subclass wants to keep going.
                    //
                    // We simply record the new chunk if nothing else is to be done.
                    boolean tooBig = accumulator.fileSize() > stateMachine.getGiveupSz();
                    boolean timedOut = stateMachine.shouldBeginTrickle();

                    if (tooBig || (timedOut && !stateMachine.isBufferAndTrickle())) {
                        // Passthru DATA Sent
                        if (tooBig) {
                            txLog.add("Mail too big for scanning.  Begin trickle");
                        } else {
                            txLog.add("Mail timed-out w/o needing to buffer (trickle, not buffer-and-trickle)");
                        }
                        // Disable tokens from client until we get the disposition to the DATA command
                        stateMachine.disableClientTokens();
                        // Send the DATA command to the server
                        txLog.add("Send synthetic DATA to server, continue when response arrives");
                        // Use the contnuation shared by this and the T_B_READING_MAIL state. The
                        // continuations are 99% the same, except they differ in their next state upon success
                        stateMachine.sendCommandToServer(new Command(CommandType.DATA), new ResponseCompletion()
                        {
                            @Override
                            public void handleResponse(Response resp, TokenResultBuilder ts)
                            {
                                handleMsgIncompleteDataContinuation(resp, ts, stateMachine, BufTxState.DRAIN_MAIL);
                            }
                        }, ts);
                        changeState(BufTxState.DONE);
                    } else if (timedOut && stateMachine.isBufferAndTrickle()) {
                        // T&B DATA Sent
                        txLog.add("Mail timed out.  Begin trickle and buffer");
                        // Disable client until we can hear back from the "DATA" command
                        stateMachine.disableClientTokens();
                        // Send the DATA command to the server and set-up the callback
                        txLog.add("Send synthetic DATA to server, continue when response arrives");
                        stateMachine.sendCommandToServer(new Command(CommandType.DATA), new ResponseCompletion()
                        {
                            @Override
                            public void handleResponse(Response resp, TokenResultBuilder ts)
                            {
                                handleMsgIncompleteDataContinuation(resp, ts, stateMachine, BufTxState.T_B_READING_MAIL);
                            }
                        }, ts);
                        changeState(BufTxState.DONE);
                    } else {
                        // The chunk is already recorded. Nothing to do. No delta state
                    }
                }// ENDOF Not Last Token

                break;
            case DRAIN_MAIL:
                // Page 25
                txLog.add("Pass this chunk on to the server");
                if (isLast) {
                    // Make sure we're no longer the active transaction
                    stateMachine.transactionEnded(this);

                    // Install the simple callback handler (doesn't do much)
                    stateMachine.sendFinalMIMEToServer(continuedToken, new ResponseCompletion()
                    {
                        @Override
                        public void handleResponse(Response resp, TokenResultBuilder ts)
                        {
                            handleMailTransmissionContinuation(resp, ts, stateMachine);
                        }
                    }, ts);

                    changeState(BufTxState.DONE);
                } else {
                    // No change in state, and the chunk has already been passed along.
                    stateMachine.sendContinuedMIMEToServer(continuedToken, ts);
                }
                break;
            case PASSTHRU_PENDING_NACK:
                // Page 27
                // Let the token fall on the floor, as it'll never be accepted by the server.
                // We're just combing through the junk from the client getting to the point where we can NACK
                if (isLast) {
                    // Make sure we're no longer the active transaction
                    stateMachine.transactionEnded(this);
                    // Transaction Failed
                    getTransaction().failed();
                    // Pass along same error (whatever it was) to client
                    txLog.add("End of queued NACK.  Send " + dataResp + " to client");
                    stateMachine.appendSyntheticResponse(new Response(dataResp, ""), immediateActions);
                    // We're done
                    changeState(BufTxState.DONE);
                } else {
                    // Nothing interesting to do.
                }
                break;
            case T_B_READING_MAIL:
                // Page 29
                // Write it to the file regardless
                appendChunk(continuedToken);
                if (isLast) {
                    txLog.add("Trickle and buffer.  Whole message obtained.  Evaluate");
                    BlockOrPassResult action = evaluateMessage(false, stateMachine);

                    switch (action) {
                        case PASS:
                            txLog.add("Evaluation passed");
                            stateMachine.sendFinalMIMEToServer(continuedToken, new ResponseCompletion()
                            {
                                @Override
                                public void handleResponse(Response resp, TokenResultBuilder ts)
                                {
                                    handleMailTransmissionContinuation(resp, ts, stateMachine);
                                }
                            }, ts);
                            break;
                        case DROP:
                            // Block, the hard way...
                            txLog.add("Evaluation failed.  Send a \"fake\" 250 to client then shutdown server");
                            stateMachine.appendSyntheticResponse(new Response(250, "OK"), immediateActions);
                            stateMachine.transactionEnded(this);
                            // Put a Response handler here, in case the goofy server
                            // sends an ACK to the FIN (which Exim seems to do!?!)
                            stateMachine.sendFINToServer(NOOP_RESPONSE_COMPLETION);
                            txLog.add("Replace SessionHandler with shutdown dummy");
                            stateMachine.startShutingDown();

                            break;
                        case TEMPORARILY_REJECT:
                            // Block, the hard way...
                            txLog.add("Evaluation failed.  Send a \"fake\" 451 to client then shutdown server");
                            stateMachine.appendSyntheticResponse(new Response(451, "Please try again later"),
                                    immediateActions);
                            stateMachine.transactionEnded(this);
                            // Put a Response handler here, in case the goofy server
                            // sends an ACK to the FIN (which Exim seems to do!?!)
                            stateMachine.sendFINToServer(NOOP_RESPONSE_COMPLETION);
                            txLog.add("Replace SessionHandler with shutdown dummy");
                            stateMachine.startShutingDown();

                            break;
                        default:
                            logger.warn("unhandled action: " + action);
                            break;
                    }
                } else {
                    stateMachine.sendContinuedMIMEToServer(continuedToken, ts);
                }
                break;
            default:
                txLog.add("Error - Unknown State " + state);
                changeState(BufTxState.DONE);
                stateMachine.transactionEnded(this);
                appendChunk(continuedToken);
                if (isLast) {
                    stateMachine.sendContinuedMIMEToServer(continuedToken, ts);
                } else {
                    stateMachine.sendFinalMIMEToServer(continuedToken, PASSTHRU_RESPONSE_COMPLETION, ts);
                }
        }
    }

    /**
     * action taken on the response callback, after evaluating a message as PASS
     * 
     * @param stateMachine
     * @param resp
     * @param ts
     */
    private void handleResponseAfterPassedMessage(final SmtpStateMachine stateMachine, Response resp,
            TokenResultBuilder ts)
    {
        logReceivedResponse(resp);
        txLog.add("Response to DATA command was " + resp.getCode());

        // Save this in a variable, so we can pass it along later (if not positive)
        dataResp = resp.getCode();

        stateMachine.enableClientTokens();
        stateMachine.transactionEnded(SmtpTransactionHandler.this);

        if (resp.getCode() < 400) {
            // Don't forward to client

            // Pass either complete MIME or begin/end (if there was a parse error)
            if (msg == null) {
                txLog.add("Passing along an unparsable MIME message in two tokens");
                isMessageMaster = false;
                stateMachine.sendBeginMIMEToServer(new BeginMIMEToken(accumulator, messageInfo), ts);
                stateMachine.sendFinalMIMEToServer(new ContinuedMIMEToken(accumulator.createChunk(null, true)),
                        new ResponseCompletion()
                        {
                            @Override
                            public void handleResponse(Response resp, TokenResultBuilder ts)
                            {
                                handleMailTransmissionContinuation(resp, ts, stateMachine);
                            }
                        }, ts);
                accumulator = null;
            } else {
                txLog.add("Passing along parsed MIME in one token");
                if (accumulator != null) {
                    accumulator.closeInput();
                    accumulator = null;
                }
                stateMachine.sentWholeMIMEToServer(new CompleteMIMEToken(msg, messageInfo),
                        new ResponseCompletion()
                        {
                            @Override
                            public void handleResponse(Response resp, TokenResultBuilder ts)
                            {
                                handleMailTransmissionContinuation(resp, ts, stateMachine);
                            }
                        }, ts);
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
            stateMachine.sendResponseToClient(resp, ts);

            // We're done
            changeState(BufTxState.DONE);
        }
    }

    // If null, just ignore
    private void appendChunk(ContinuedMIMEToken continuedToken)
    {
        if (continuedToken == null) {
            return;
        }
        if (accumulator == null) {
            logger.error("Received ContinuedMIMEToken without a MIMEAccumulator set");
            return;
        }
        if (!accumulator.appendChunkToFile(continuedToken.getMIMEChunk())) {
            logger.error("Error appending MIME Chunk");
        }

    }

    private void changeState(BufTxState newState)
    {
        logRecordStateChange(state, newState);
        state = newState;
    }

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
     */
    private BlockOrPassResult evaluateMessage(boolean canModify, SmtpStateMachine stateMachine)
    {
        if (msg == null) {
            msg = accumulator.parseBody(messageInfo);
            accumulator.closeInput();
            if (msg == null) {
                txLog.add("Parse error on MIME.  Assume it passed scanning");
                return BlockOrPassResult.PASS;
            }
        }
        if (canModify) {
            ScannedMessageResult result = stateMachine.blockPassOrModify(msg, getTransaction(), messageInfo);
            if (result.messageModified()) {
                txLog.add("Evaluation modified MIME message");
                msg = result.getMessage();
                return BlockOrPassResult.PASS;
            } else {
                return result.getAction();
            }
        } else {
            return stateMachine.blockOrPass(msg, getTransaction(), messageInfo);
        }
    }

    public void handleMailTransmissionContinuation(Response resp, TokenResultBuilder ts, SmtpStateMachine stateMachine)
    {
        logReceivedResponse(resp);
        txLog.add("Response to mail transmission command was " + resp.getCode());

        if (resp.getCode() < 300) {
            getTransaction().commit();
        } else {
            getTransaction().failed();
        }
        changeState(BufTxState.DONE);
        stateMachine.transactionEnded(SmtpTransactionHandler.this);
        logger.debug("Sending response " + resp.getCode() + " to client");
        ts.addTokenForClient(resp);
    }

    public void handleMsgIncompleteDataContinuation(Response resp, TokenResultBuilder ts,
            SmtpStateMachine stateMachine, BufTxState nextStateIfPositive)
    {

        logReceivedResponse(resp);
        txLog.add("Response to DATA command was " + resp.getCode());

        // Save this in a variable, so we can pass it along later (if not positive)
        dataResp = resp.getCode();

        stateMachine.enableClientTokens();

        if (resp.getCode() < 400) {
            txLog.add("Begin trickle with BeginMIMEToken");
            stateMachine.sendBeginMIMEToServer(new BeginMIMEToken(accumulator, messageInfo), ts);
            isMessageMaster = false;
            changeState(nextStateIfPositive);
        } else {
            // Discard message
            closeMessageResources(false);

            stateMachine.sendCommandToServer(new Command(CommandType.RSET), NOOP_RESPONSE_COMPLETION, ts);
            changeState(BufTxState.PASSTHRU_PENDING_NACK);
        }
    }

    // ============== Logging ==========================

    void logReceivedToken(Token token)
    {
        txLog.add("----Received Token " + token + "--------");
    }

    void logReceivedResponse(Response resp)
    {
        txLog.add("----Received Response " + resp.getCode() + "--------");
    }

    void logRecordStateChange(BufTxState old, BufTxState newState)
    {
        txLog.add("----Change State " + old + "->" + newState + "-----------");
    }

    void dumpToLogger(Level level)
    {
        logger.log(level, "=======BEGIN Transaction Log=============");
        for (String s : txLog) {
            logger.log(level, s);
        }
        logger.log(level, "=======ENDOF Transaction Log=============");
        txLog.clear();
    }
}
