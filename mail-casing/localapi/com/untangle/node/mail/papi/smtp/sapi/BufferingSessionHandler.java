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
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.BeginMIMEToken;
import com.untangle.node.mail.papi.CompleteMIMEToken;
import com.untangle.node.mail.papi.ContinuedMIMEToken;
import com.untangle.node.mail.papi.MIMEAccumulator;
import com.untangle.node.mail.papi.MessageInfo;
import com.untangle.node.mail.papi.MessageTransmissionTimeoutStrategy;
import com.untangle.node.mail.papi.smtp.Command;
import com.untangle.node.mail.papi.smtp.MAILCommand;
import com.untangle.node.mail.papi.smtp.RCPTCommand;
import com.untangle.node.mail.papi.smtp.Response;
import com.untangle.node.mail.papi.smtp.SmtpTransaction;
import com.untangle.node.mime.EmailAddress;
import com.untangle.node.mime.MIMEMessage;
import com.untangle.node.token.Token;

/**
 * Subclass of SessionHandler which yet-again simplifies consumption
 * of an SMTP stream.  This class was created for Nodes wishing only
 * to see "a whole mail".
 * <br><br>
 * This class <i>buffers</i> mails, meaning it does not pass each MIME
 * chunk to the server.  Instead, it attempts to collect them into a
 * file and present them to the {@link #blockPassOrModify
 * blockPassOrModify} method for evaluation.
 * <br><br>
 * There are two cases when the {@link #blockPassOrModify
 * blockPassOrModify} method will not be called for a given mail
 * (except for when a given transaction is aborted, but the subclass
 * does not even see such aborts).  The first is if the subclass is
 * only interested in mails {@link #getGiveupSz below a certain size}.
 * This size is declared by implementing the {@link #getGiveupSz
 * getGiveupSz()} method.
 * <br><br>
 * The second case which prevents {@link #blockPassOrModify
 * blockPassOrModify()} from being called is when this handler has
 * begun to <i>trickle</i>.  Trickling is the state in which the
 * BufferingSessionHandler passes MIME chunks to the server as they
 * arrive.  Tricking is initiated after the BufferingSessionHandler
 * determines either the client or server is in danger of timing-out.
 * The timeout time is set by the subclass via the {@link
 * #getMaxClientWait getMaxClientWait()} and {@link #getMaxServerWait
 * getMaxServerWait()} methods.
 * <br><br>
 * When timeout occurs, the BufferingSessionHandler can enter one of
 * two states.  It can <i>giveup-then-trickle</i>, meaning no
 * evaluation will take place.  Alternatly, it can enter
 * <i>buffer-and-trickle</i> meaning it will continue to buffer while
 * bytes are sent to the server.  Which state is entered after timeout
 * is determined by the subclass' return of the {@link
 * #isBufferAndTrickle isBufferAndTrickle()} method.
 * <br><br>
 * If <i>buffer-and-trickle</i> is selected, the method {@link
 * #blockOrPass blockOrPass()} will be invoked once the whole mail is
 * observed.  Note that modification of the MIME message is forbidden
 * in the {@link #blockOrPass blockOrPass()} callback, as it is too
 * late to modify the message.
 */
public abstract class BufferingSessionHandler
    extends SessionHandler {

    private static final String RESP_TXT_354 = "Start mail input; end with <CRLF>.<CRLF>";


    //================================
    // Public Inner Classes
    //================================

    /**
     * Actions the subclass can take when
     * message modification is not an action.
     * The enum is easier than remembering if true
     * means "block" or "don't block"
     */
    public enum BlockOrPassResult {
        BLOCK,
        PASS,
        TEMPORARILY_REJECT
    };


    /**
     * <b>B</b>lock, <b>P</b>ass, or <b>M</b>odify Evaluation result.
     */
    public static final class BPMEvaluationResult {
        private MIMEMessage m_newMsg;
        private final BlockOrPassResult action;


        private BPMEvaluationResult(BlockOrPassResult action)
        {
            this.action = action;
        }

        /**
         * Constrctor used to create a result
         * indicating that the message has been
         * modified.  This implicitly is not
         * a block.
         */
        public BPMEvaluationResult(MIMEMessage newMsg) {
            action = BlockOrPassResult.PASS;
            m_newMsg = newMsg;
        }

        public boolean isBlock() {
            return action == BlockOrPassResult.BLOCK;
        }

        public BlockOrPassResult getAction()
        {
            return action;
        }

        public boolean messageModified() {
            return m_newMsg != null;
        }

        public MIMEMessage getMessage() {
            return m_newMsg;
        }
    }
    /**
     * Result from {@link #blockPassOrModify blockPassOrModify} indicating block message
     */
    public static BPMEvaluationResult BLOCK_MESSAGE = new BPMEvaluationResult(BlockOrPassResult.BLOCK);
    /**
     * Result from {@link #blockPassOrModify blockPassOrModify} indicating pass message
     */
    public static BPMEvaluationResult PASS_MESSAGE = new BPMEvaluationResult(BlockOrPassResult.PASS);

    public static BPMEvaluationResult TEMPORARILY_REJECT = new BPMEvaluationResult(BlockOrPassResult.TEMPORARILY_REJECT);

    private final Logger m_logger = Logger.getLogger(BufferingSessionHandler.class);

    private final int m_giveupSz;
    private final long m_maxClientWait;
    private final long m_maxServerWait;
    private final boolean m_isBufferAndTrickle;
    private String m_heloName = null;


    protected BufferingSessionHandler(int giveUpSz,
                                      long maxClientWait,
                                      long maxServerWait,
                                      boolean isBufferAndTrickle) {
        m_giveupSz = giveUpSz;
        m_maxClientWait = maxClientWait<=0?Integer.MAX_VALUE:maxClientWait;
        m_maxServerWait = maxServerWait<=0?Integer.MAX_VALUE:maxServerWait;
        m_isBufferAndTrickle = isBufferAndTrickle;
    }

    //==================================
    // (these were abstract)
    //==================================

    /**
     * Get the size over-which this class should no
     * longer buffer.  After this point, the
     * Buffering is abandoned and the
     * <i>giveup-then-trickle</i> state is entered.
     */
    protected final int getGiveupSz() {
        return m_giveupSz;
    }

    /**
     * The maximum time (in relative milliseconds)
     * that the client can wait for a response
     * to DATA transmission.
     */
    protected final long getMaxClientWait() {
        return m_maxClientWait;
    }

    /**
     * The maximum time that the server can wait
     * for a subsequent ("DATA") command.
     */
    protected final long getMaxServerWait() {
        return m_maxServerWait;
    }

    /**
     * If true, this handler will continue to buffer even after
     * trickling has begun (<i>buffer-and-trickle</i> mode).
     * The MIMEMessage can no longer be modified
     * (i.e. it will not be passed downstream) but
     * {@link #blockOrPass blockOrPass()} will still be called
     * once the complete message has been seen.
     */
    protected final boolean isBufferAndTrickle() {
        return m_isBufferAndTrickle;
    }

    /**
     * Callback once an entire mail has been buffered.  Subclasses
     * can choose one of the three permitted outcomes (Block, pass, modify).
     *
     * @param msg the MIMEMessage
     * @param tx the transaction
     * @param msgInfo the MessageInfo (for creating reporting events).
     */
    public abstract BPMEvaluationResult blockPassOrModify(MIMEMessage msg,
                                                          SmtpTransaction tx,
                                                          MessageInfo msgInfo);

    /**
     * Callback once a complete message has been buffered,
     * after the BufferingSessionHandler already entered
     * <i>buffer-and-trickle</i> mode.  The message
     * cannot be modified, but it can still be blocked (and
     * any report events can be sent).
     * <br><br>
     * Note that this method is called <i>just before</i>
     * the last chunk of the message is passed-along to
     * the server (if it were afterwards, there would be
     * no ability to block!).
     *
     *
     * @param msg the MIMEMessage
     * @param tx the transaction
     * @param msgInfo the MessageInfo (for creating reporting events).
     */
    public abstract BlockOrPassResult blockOrPass(MIMEMessage msg,
                                                  SmtpTransaction tx,
                                                  MessageInfo msgInfo);


    private BPMEvaluationResult callBlockPassOrModify(MIMEMessage msg,
                                                      SmtpTransaction tx,
                                                      MessageInfo msgInfo) {

        try {
            return blockPassOrModify(msg, tx, msgInfo);
        }
        catch(Throwable t) {
            m_logger.error("Exception calling subclass.  Pass message", t);
            return PASS_MESSAGE;
        }
    }


    private BlockOrPassResult callBlockOrPass(MIMEMessage msg,
                                              SmtpTransaction tx,
                                              MessageInfo msgInfo) {

        try {
            return blockOrPass(msg, tx, msgInfo);
        }
        catch(Throwable t) {
            m_logger.error("Exception calling subclass.  Pass message", t);
            return BlockOrPassResult.PASS;
        }
    }


    /**
     * Get the name fhe client used on a HELO/EHLO line.
     * This *should* be the remote client's hostname
     *
     * @return the name String
     */
    protected String getHELOEHLOName() {
        return m_heloName;
    }


    //================================
    // SessionHandler methods
    //================================

    @Override
    public final void handleCommand(Command command,
                                    Session.SmtpCommandActions actions) {

        m_logger.debug("[handleCommand] with command of type \"" +
                       command.getType() + "\"");

        actions.sendCommandToServer(command, new PassthruResponseCompletion());
    }

    @Override
    public void observeEHLOCommand(Command cmd) {
        m_heloName = cmd.getArgString();
    }

    @Override
    public final TransactionHandler createTxHandler(SmtpTransaction tx) {
        return new BufferingTransactionHandler(tx);
    }

    @Override
    public boolean handleServerFIN(TransactionHandler currentTX) {
        return true;
    }

    @Override
    public boolean handleClientFIN(TransactionHandler currentTX) {
        return true;
    }

    @Override
    public void handleFinalized() {
        //
    }


    //==========================
    // Helpers
    //==========================

    /**
     * Determines, based on timestamps, if
     * trickling should begin
     */
    private boolean shouldBeginTrickle() {

        return MessageTransmissionTimeoutStrategy.inTimeoutDanger(
                                                                  Math.min(getMaxClientWait(), getMaxServerWait()),
                                                                  Math.min(
                                                                           getSession().getLastClientTimestamp(),
                                                                           getSession().getLastServerTimestamp()));
    }


    //===================== Inner Class ====================

    /**
     * Possible states for the Transactions
     */
    private enum BufTxState {
        INIT,
        GATHER_ENVELOPE,
        BUFFERING_MAIL,
        PASSTHRU_DATA_SENT,
        T_B_DATA_SENT,
        BUFFERED_DATA_SENT,
        BUFFERED_TRANSMITTED_WAIT_REPLY,
        PASSTHRU_PENDING_NACK,
        DRAIN_MAIL,
        DRAIN_MAIL_WAIT_REPLY,
        T_B_READING_MAIL,
        DONE
    };


    //===================== Inner Class ====================

    /**
     * Log of the transaction (not to be confused with database TX logs).
     * Simply a sequential dialog of what took place, so if there is
     * some problem we can send this log to the real log file.
     */
    private class TxLog extends ArrayList<String> {

        void receivedToken(Token token) {
            add("----Received Token " + token + "--------");
        }
        void receivedResponse(Response resp) {
            add("----Received Response " + resp.getCode() + "--------");
        }
        void recordStateChange(BufTxState old, BufTxState newState) {
            add("----Change State " + old + "->" + newState + "-----------");
        }
        void dumpToLogger(Logger logger,Level level) {
            logger.log(level,"=======BEGIN Transaction Log=============");
            for(String s : this) {
                logger.log(level,s);
            }
            logger.log(level,"=======ENDOF Transaction Log=============");
            clear();
        }

        //TODO bscott this is temp, just for debugging
        @Override
        public boolean add(String s) {
            m_logger.debug("[TX-LOG] " + s);
            return super.add(s);
        }

    }

    //===================== Inner Class ====================

    /**
     * The main workhorse of this class.
     */
    private class BufferingTransactionHandler
        extends TransactionHandler {

        private TxLog m_txLog = new TxLog();
        private BufTxState m_state = BufTxState.INIT;

        private MIMEAccumulator m_accumulator;
        private MessageInfo m_messageInfo;
        private MIMEMessage m_msg;
        private boolean m_isMessageMaster;//Flag indicating if this handler
        //is the "master" of the message (accumulator
        //or MIMEMessage).  The master is defined
        //as the last node to receive the object
        //in question.  The master designation is set
        //once a Begin/complete token is received, and
        //relinquished once any BEGIN/Complete
        //token is passed

        private int m_dataResp = -1;//Special case for when we get a negative
        //response to our sending of a "real" DATA
        //command to the server.  In this special
        //case, we need to queue what the server said
        //to the DATA command and instead send it
        //as a result of the client's <CRLF>.<CRLF>

        BufferingTransactionHandler(SmtpTransaction tx) {
            super(tx);
            m_txLog.add("---- Initial state " + m_state + " (" +
                        new Date() + ") -------");
            m_isMessageMaster = false;
        }


        //TODO bscott Nuke the file if we were accumulating MIME.  This means
        //     we need to handle the "client" and "server" close stuff

        @Override
        public void handleRSETCommand(Command command,
                                      Session.SmtpCommandActions actions) {

            m_txLog.receivedToken(command);

            //Look for the state we understand.  Note I included "INIT"
            //but that should be impossible by definition
            if(m_state == BufTxState.GATHER_ENVELOPE ||
               m_state == BufTxState.INIT) {
                m_txLog.add("Aborting at client request");
                actions.transactionEnded(this);
                getTransaction().reset();
                actions.sendCommandToServer(command, new PassthruResponseCompletion());
                changeState(BufTxState.DONE);
                finalReport();
                closeMessageResources(false);
            }
            else {//State/command misalignment
                m_txLog.add("Impossible command now: \"" + command + "\"");
                m_txLog.dumpToLogger(m_logger,Level.ERROR);
                actions.transactionEnded(this);
                actions.sendCommandToServer(command, new PassthruResponseCompletion());
                changeState(BufTxState.DONE);
            }
        }

        @Override
        public void handleCommand(Command command,
                                  Session.SmtpCommandActions actions) {
            m_txLog.receivedToken(command);

            if(m_state == BufTxState.GATHER_ENVELOPE ||
               m_state == BufTxState.INIT) {
                if(command.getType() == Command.CommandType.DATA) {
                    //Don't passthru
                    m_txLog.add("Enqueue synthetic 354 for client");
                    actions.appendSyntheticResponse(new FixedSyntheticResponse(354, RESP_TXT_354));
                    changeState(BufTxState.BUFFERING_MAIL);
                }
                else {
                    m_txLog.add("Passthru to client");
                    actions.sendCommandToServer(command, new PassthruResponseCompletion());
                }
            }
            else {
                m_txLog.add("Impossible command now: \"" + command + "\"");
                m_txLog.dumpToLogger(m_logger,Level.ERROR);
                actions.sendCommandToServer(command, new PassthruResponseCompletion());
                actions.transactionEnded(this);
                changeState(BufTxState.DONE);
            }
        }

        private void handleMAILOrRCPTCommand(Command command,
                                             Session.SmtpCommandActions actions,
                                             ResponseCompletion compl) {

            m_txLog.receivedToken(command);

            if(m_state == BufTxState.GATHER_ENVELOPE ||
               m_state == BufTxState.INIT) {
                if(m_state == BufTxState.INIT) {
                    changeState(BufTxState.GATHER_ENVELOPE);
                }
                m_txLog.add("Pass " + command.getType() + " command to server, register callback " +
                            " to modify envelope at response");
                actions.sendCommandToServer(command, compl);
            }
            else {
                m_txLog.add("Impossible command now: \"" + command + "\"");
                m_txLog.dumpToLogger(m_logger,Level.ERROR);
                actions.sendCommandToServer(command, new PassthruResponseCompletion());
                actions.transactionEnded(this);
                changeState(BufTxState.DONE);
            }
        }

        @Override
        public void handleMAILCommand(MAILCommand command,
                                      Session.SmtpCommandActions actions) {
            getTransaction().fromRequest(command.getAddress());
            handleMAILOrRCPTCommand(command, actions, new MAILContinuation(command.getAddress()));
        }

        @Override
        public void handleRCPTCommand(RCPTCommand command,
                                      Session.SmtpCommandActions actions) {
            getTransaction().toRequest(command.getAddress());
            handleMAILOrRCPTCommand(command, actions, new RCPTContinuation(command.getAddress()));
        }



        @Override
        public void handleBeginMIME(BeginMIMEToken token,
                                    Session.SmtpCommandActions actions) {

            m_txLog.receivedToken(token);

            m_accumulator = token.getMIMEAccumulator();
            m_messageInfo = token.getMessageInfo();
            m_isMessageMaster = true;

            handleMIMEChunk(true,
                            false,
                            null,
                            actions);
        }

        @Override
            public void handleContinuedMIME(ContinuedMIMEToken token,
                                            Session.SmtpCommandActions actions) {
            m_txLog.receivedToken(token);

            handleMIMEChunk(false,
                            token.isLast(),
                            token,
                            actions);
        }

        @Override
        public void handleCompleteMIME(CompleteMIMEToken token,
                                       Session.SmtpCommandActions actions) {

            m_txLog.receivedToken(token);

            m_msg = token.getMessage();
            m_messageInfo = token.getMessageInfo();
            m_isMessageMaster = true;
            //TODO bscott Should we close the file of accumulated MIME?  It is really
            //     an error to have the file at all
            m_accumulator = null;

            handleMIMEChunk(true, true, null, actions);

            //      m_messageInfo = null;
        }
        @Override
        public void handleFinalized() {
            closeMessageResources(true);
        }


        private void handleMIMEChunk(boolean isFirst,
                                     boolean isLast,
                                     ContinuedMIMEToken continuedToken,/*Odd semantics - may be null*/
                                     Session.SmtpCommandActions actions) {


            switch(m_state) {
            case INIT:
            case GATHER_ENVELOPE:
            case BUFFERED_DATA_SENT:
            case BUFFERED_TRANSMITTED_WAIT_REPLY:
            case PASSTHRU_DATA_SENT:
            case DRAIN_MAIL_WAIT_REPLY:
            case T_B_DATA_SENT:
            case DONE:
                //TODO bscott handle this case better.  Dump anything we have first and
                //     declare passthru
                m_txLog.add("Impossible command now: MIME chunk (first? " + isFirst +
                            ", isLast? " + isLast);
                m_txLog.dumpToLogger(m_logger,Level.ERROR);
                appendChunk(continuedToken);
                changeState(BufTxState.DONE);
                break;
            case BUFFERING_MAIL:
                //------Page 21--------
                if(isLast) {
                    //We have the complete message.
                    m_txLog.add("Have whole message.  Evaluate");
                    if(continuedToken != null) {
                        appendChunk(continuedToken);
                    }

                    BlockOrPassResult action = evaluateMessage(true);
                    switch (action) {
                    case PASS:
                        m_txLog.add("Message passed evaluation");
                        //We're passing the message (or there was a silent parser error)

                        //Disable client tokens while we "catch-up" by issuing the DATA
                        //command
                        actions.disableClientTokens();

                        m_txLog.add("Send synthetic DATA to server, continue when reply arrives");
                        actions.sendCommandToServer(new Command(Command.CommandType.DATA),
                                                    new BufferedDATARequestContinuation());

                        changeState(BufTxState.BUFFERED_DATA_SENT);//TODO bscott Useless state.
                        //It only ever gets callback and does some reporting

                        break;
                    case BLOCK:
                        //TODO bscott is it safe to nuke the accumulator and/or message
                        //     here, or do we wait for the callback?
                        //We're blocking the message
                        m_txLog.add("Message failed evaluation (we're going to block it)");
                        //Send a fake 250 to client
                        m_txLog.add("Enqueue synthetic 250 to client");
                        actions.appendSyntheticResponse(new FixedSyntheticResponse(250, "OK"));

                        //Send REST to server (ignore result)
                        m_txLog.add("Send synthetic RSET to server (ignoring the response)");
                        actions.sendCommandToServer(new Command(Command.CommandType.RSET),
                                                    new NoopResponseCompletion());

                        changeState(BufTxState.DONE);
                        actions.transactionEnded(this);
                        getTransaction().reset();
                        finalReport();

                        break;
                    case TEMPORARILY_REJECT:
                        //We're blocking the message
                        m_txLog.add("Message temporarily rejected");
                        //Send a fake 451 to client
                        m_txLog.add("Enqueue synthetic 451 to client");
                        actions.appendSyntheticResponse(new FixedSyntheticResponse(451, "Please try again later"));

                        //Send REST to server (ignore result)
                        m_txLog.add("Send synthetic RSET to server (ignoring the response)");
                        actions.sendCommandToServer(new Command(Command.CommandType.RSET),
                                                    new NoopResponseCompletion());

                        changeState(BufTxState.DONE);
                        actions.transactionEnded(this);
                        getTransaction().reset();
                        finalReport();

                        break;
                    default:
                        m_logger.warn("unhandled action: " + action);
                        break;
                    }
                } else {
                    m_txLog.add("Not last MIME chunk, append to the file");
                    appendChunk(continuedToken);
                    //We go down one of three branches
                    //from here.  We begin passthru if the
                    //mail is too large or if we've timed out.
                    //
                    //We Trickle&Buffer if we've timed out
                    //but the subclass wants to keep going.
                    //
                    //We simply record the new chunk if nothing
                    //else is to be done.
                    boolean tooBig = m_accumulator.fileSize() > getGiveupSz();
                    boolean timedOut = shouldBeginTrickle();

                    if(tooBig || (timedOut && !isBufferAndTrickle())) {
                        //Passthru DATA Sent
                        if(tooBig) {
                            m_txLog.add("Mail too big for scanning.  Begin trickle");
                        }
                        else {
                            m_txLog.add("Mail timed-out w/o needing to buffer (trickle, not buffer-and-trickle)");
                        }
                        //Disable tokens from client until we get the disposition to the DATA command
                        actions.disableClientTokens();
                        //Send the DATA command to the server
                        m_txLog.add("Send synthetic DATA to server, continue when response arrives");
                        //Use the contnuation shared by this and the T_B_READING_MAIL state.  The
                        //continuations are 99% the same, except they differ in their
                        //next state upon success
                        actions.sendCommandToServer(new Command(Command.CommandType.DATA),
                                                    new MsgIncompleteDATARequestContinuation(BufTxState.DRAIN_MAIL));
                        changeState(BufTxState.PASSTHRU_DATA_SENT);
                    }
                    else if(timedOut && isBufferAndTrickle()) {
                        //T&B DATA Sent
                        m_txLog.add("Mail timed out.  Begin trickle and buffer");
                        //Disable client until we can hear back from the "DATA" command
                        actions.disableClientTokens();
                        //Send the DATA command to the server and set-up the callback
                        m_txLog.add("Send synthetic DATA to server, continue when response arrives");
                        actions.sendCommandToServer(new Command(Command.CommandType.DATA),
                                                    new MsgIncompleteDATARequestContinuation(BufTxState.T_B_READING_MAIL));
                        changeState(BufTxState.T_B_DATA_SENT);
                    }
                    else {
                        //The chunk is already recorded.  Nothing to do.  No delta state
                    }
                }//ENDOF Not Last Token

                break;
            case DRAIN_MAIL:
                //Page 25
                //TODO bscott how can I tell this won't be null?
                m_txLog.add("Pass this chunk on to the server");
                if(isLast) {
                    //Make sure we're no longer the active transaction
                    actions.transactionEnded(BufferingTransactionHandler.this);

                    //Install the simple callback handler (doesn't do much)
                    actions.sendFinalMIMEToServer(continuedToken, new MailTransmissionContinuation());

                    changeState(BufTxState.DRAIN_MAIL_WAIT_REPLY);//TODO bscott a stupid state (not needed)
                }
                else {
                    //Nothing interesting to do.  No change in state,
                    //and the chunk has already been passed along.
                    actions.sendContinuedMIMEToServer(continuedToken);
                }
                break;
            case PASSTHRU_PENDING_NACK:
                //Page 27
                //Let the token fall on the floor, as it'll never be accepted by
                //the server.  We're just combing through the junk from the client
                //getting to the point where we can NACK
                if(isLast) {
                    //Make sure we're no longer the active transaction
                    actions.transactionEnded(BufferingTransactionHandler.this);
                    //Transaction Failed
                    getTransaction().failed();
                    //Pass along same error (whatever it was) to client
                    m_txLog.add("End of queued NACK.  Send " + m_dataResp + " to client");
                    actions.appendSyntheticResponse(new FixedSyntheticResponse(m_dataResp, ""));
                    //We're done
                    changeState(BufTxState.DONE);
                    finalReport();
                }
                else {
                    //Nothing interesting to do.
                }
                break;
            case T_B_READING_MAIL:
                //Page 29
                //Write it to the file regardless
                appendChunk(continuedToken);
                if(isLast) {
                    m_txLog.add("Trickle and buffer.  Whole message obtained.  Evaluate");
                    BlockOrPassResult action = evaluateMessage(false);

                    switch (action) {
                    case PASS:
                        m_txLog.add("Evaluation passed");
                        actions.sendFinalMIMEToServer(continuedToken,
                                                      new MailTransmissionContinuation());
                        break;
                    case BLOCK:
                        //Block, the hard way...
                        m_txLog.add("Evaluation failed.  Send a \"fake\" 250 to client then shutdown server");
                        actions.appendSyntheticResponse(new FixedSyntheticResponse(250, "OK"));
                        actions.transactionEnded(this);
                        //Put a Response handler here, in case the goofy server
                        //sends an ACK to the FIN (which Exim seems to do!?!)
                        actions.sendFINToServer(new NoopResponseCompletion());
                        m_txLog.add("Replace SessionHandler with shutdown dummy");
                        getSession().setSessionHandler(new ShuttingDownSessionHandler(1000*60));//TODO bscott a real timeout value

                        break;
                    case TEMPORARILY_REJECT:
                        //Block, the hard way...
                        m_txLog.add("Evaluation failed.  Send a \"fake\" 451 to client then shutdown server");
                        actions.appendSyntheticResponse(new FixedSyntheticResponse(451, "Please try again later"));
                        actions.transactionEnded(this);
                        //Put a Response handler here, in case the goofy server
                        //sends an ACK to the FIN (which Exim seems to do!?!)
                        actions.sendFINToServer(new NoopResponseCompletion());
                        m_txLog.add("Replace SessionHandler with shutdown dummy");
                        getSession().setSessionHandler(new ShuttingDownSessionHandler(1000*60));//TODO bscott a real timeout value

                        break;
                    default:
                        m_logger.warn("unhandled action: " + action);
                        break;
                    }
                } else {
                    actions.sendContinuedMIMEToServer(continuedToken);
                }
                break;
            default:
                m_txLog.add("Error - Unknown State " + m_state);
                changeState(BufTxState.DONE);
                actions.transactionEnded(this);
                appendChunk(continuedToken);
                if(isLast) {
                    actions.sendContinuedMIMEToServer(continuedToken);
                }
                else {
                    actions.sendFinalMIMEToServer(continuedToken, new PassthruResponseCompletion());
                }
            }
        }


        //If null, just ignore
        private void appendChunk(ContinuedMIMEToken continuedToken) {
            if(continuedToken == null) {
                return;
            }
            if(m_accumulator == null) {
                m_logger.error("Received ContinuedMIMEToken without a MIMEAccumulator set");
            }
            if(!m_accumulator.appendChunkToFile(continuedToken.getMIMEChunk())) {
                m_logger.error("Error appending MIME Chunk");
                //I'm not going to dispose of the file at this point,
                //as it may cause other downstream NPEs.  Basically,
                //we're screwed if this occurs (and should only occur if
                //we get a disk I/O error, which is unrecoverable).
                //        if(m_isMessageMaster) {
                //          m_accumulator.dispose();
                //          m_accumulator = null;
                //        }
            }

        }


        private void changeState(BufTxState newState) {
            m_txLog.recordStateChange(m_state, newState);
            m_state = newState;
        }

        private void closeMessageResources(boolean force) {
            if(m_isMessageMaster || force) {
                if(m_accumulator != null) {
                    m_accumulator.dispose();
                    m_accumulator = null;
                }
                if(m_msg != null) {
                    m_msg.dispose();
                    m_msg = null;
                }
            }
        }

        /**
         * Helper which prints the tx log to debug
         * as a final report of what took place
         */
        private void finalReport() {
            //TODO Send TxLog to debug and add final state
            //      m_txLog.add("Final transaction state: " + getTransaction().getState());
            //      m_txLog.dumpToDebug(m_logger);
        }

        /**
         * Returns PASS if we should pass.  If there is a parsing
         * error, this also returns PASS.  If the message
         * was changed, it'll just be picked-up implicitly.
         */
        private BlockOrPassResult evaluateMessage(boolean canModify) {
            if (m_msg == null) {
                m_msg = m_accumulator.parseBody();
                m_accumulator.closeInput();
                if(m_msg == null) {
                    m_txLog.add("Parse error on MIME.  Assume it passed scanning");
                    return BlockOrPassResult.PASS;
                }
            }
            if (canModify) {
                BPMEvaluationResult result = callBlockPassOrModify(m_msg,
                                                                   getTransaction(),
                                                                   m_messageInfo);
                if(result.messageModified()) {
                    m_txLog.add("Evaluation modified MIME message");
                    m_msg = result.getMessage();
                    return BlockOrPassResult.PASS;
                } else {
                    return result.getAction();
                }
            } else {
                return callBlockOrPass(m_msg, getTransaction(), m_messageInfo);
            }
        }

        //==========================
        // Inner-Inner-Classes
        //===========================

        //****************** Inner-Inner Class Separator ******************
        /**
         * Callback when we have buffered a mail, decided to let
         * it pass, then sent a DATA to the server.
         */
        private class BufferedDATARequestContinuation
            extends PassthruResponseCompletion {

            public void handleResponse(Response resp,
                                       Session.SmtpResponseActions actions) {

                m_txLog.receivedResponse(resp);
                m_txLog.add("Response to DATA command was " + resp.getCode());

                //Save this in a variable, so we can pass it along
                //later (if not positive)
                m_dataResp = resp.getCode();

                actions.enableClientTokens();
                actions.transactionEnded(BufferingTransactionHandler.this);

                if(resp.getCode() < 400) {
                    //Don't forward to client

                    //Pass either complete MIME or begin/end (if there
                    //was a parse error)
                    if(m_msg == null) {
                        m_txLog.add("Passing along an unparsable MIME message in two tokens");
                        m_isMessageMaster = false;
                        actions.sendBeginMIMEToServer(new BeginMIMEToken(m_accumulator, m_messageInfo));
                        actions.sendFinalMIMEToServer(
                                                      new ContinuedMIMEToken(m_accumulator.createChunk(null, true)),
                                                      new MailTransmissionContinuation());
                        m_accumulator = null;
                    }
                    else {
                        m_txLog.add("Passing along parsed MIME in one token");
                        if(m_accumulator != null) {
                            m_accumulator.closeInput();
                            m_accumulator = null;
                        }
                        actions.sentWholeMIMEToServer(new CompleteMIMEToken(m_msg, m_messageInfo),
                                                      new MailTransmissionContinuation());
                        m_isMessageMaster = false;
                        m_msg = null;
                    }


                    //Change state to BUFFERED_TRANSMITTED_WAIT_REPLY
                    changeState(BufTxState.BUFFERED_TRANSMITTED_WAIT_REPLY);
                }
                else {
                    //Discard message
                    closeMessageResources(false);

                    //Transaction Failed
                    getTransaction().failed();

                    //Pass along same error (whatever it was) to client
                    actions.sendResponseToClient(resp);

                    //We're done
                    changeState(BufTxState.DONE);
                    finalReport();
                }
            }
        }

        //****************** Inner-Inner Class Separator ******************
        /**
         * Callback when a DATA request completes.  This is used
         * for the "PASSTHRU_DATA_SENT" and "T_B_DATA_SENT"
         * states, which are issuing the DATA command in advance
         * of having the complete message.
         *
         * Both of those states share the same negative
         * next state, but differ in their positive next state.
         */
        private class MsgIncompleteDATARequestContinuation
            extends PassthruResponseCompletion {

            private BufTxState m_nextState;

            MsgIncompleteDATARequestContinuation(BufTxState nextStateIfPositive) {
                m_nextState = nextStateIfPositive;
            }

            public void handleResponse(Response resp,
                                       Session.SmtpResponseActions actions) {

                m_txLog.receivedResponse(resp);
                m_txLog.add("Response to DATA command was " + resp.getCode());

                //Save this in a variable, so we can pass it along
                //later (if not positive)
                m_dataResp = resp.getCode();

                actions.enableClientTokens();

                if(resp.getCode() < 400) {
                    m_txLog.add("Begin trickle with BeginMIMEToken");
                    actions.sendBeginMIMEToServer(new BeginMIMEToken(m_accumulator, m_messageInfo));
                    m_isMessageMaster = false;
                    changeState(m_nextState);
                }
                else {
                    //Discard message
                    closeMessageResources(false);

                    actions.sendCommandToServer(new Command(Command.CommandType.RSET),
                                                new NoopResponseCompletion());
                    changeState(BufTxState.PASSTHRU_PENDING_NACK);
                }
            }
        }


        //****************** Inner-Inner Class Separator ******************

        /**
         * Callback for the server's response to the mail transmission
         * (the thing ending in <CRLF>.<CRLF>).
         */
        private class MailTransmissionContinuation
            extends PassthruResponseCompletion {

            public void handleResponse(Response resp,
                                       Session.SmtpResponseActions actions) {

                m_txLog.receivedResponse(resp);
                m_txLog.add("Response to mail transmission command was " + resp.getCode());

                if(resp.getCode() < 300) {
                    getTransaction().commit();
                }
                else {
                    getTransaction().failed();
                }
                changeState(BufTxState.DONE);
                actions.transactionEnded(BufferingTransactionHandler.this);
                finalReport();
                super.handleResponse(resp, actions);
            }
        }



        //****************** Inner-Inner Class Separator ******************

        private abstract class ContinuationWithAddress
            extends PassthruResponseCompletion {

            private final EmailAddress m_addr;

            ContinuationWithAddress(EmailAddress addr) {
                m_addr = addr;
            }

            protected EmailAddress getAddress() {
                return m_addr;
            }

            public void handleResponse(Response resp,
                                       Session.SmtpResponseActions actions) {

                super.handleResponse(resp, actions);
            }
        }


        //****************** Inner-Inner Class Separator ******************

        private class MAILContinuation
            extends ContinuationWithAddress {

            MAILContinuation(EmailAddress addr) {
                super(addr);
            }

            public void handleResponse(Response resp,
                                       Session.SmtpResponseActions actions) {
                m_txLog.receivedResponse(resp);
                m_txLog.add("Response to MAIL for address \"" +
                            getAddress() + "\" was " + resp.getCode());
                getTransaction().fromResponse(getAddress(), (resp.getCode() < 300));
                super.handleResponse(resp, actions);
            }
        }


        //****************** Inner-Inner Class Separator ******************

        private class RCPTContinuation
            extends ContinuationWithAddress {

            RCPTContinuation(EmailAddress addr) {
                super(addr);
            }

            public void handleResponse(Response resp,
                                       Session.SmtpResponseActions actions) {
                m_txLog.receivedResponse(resp);
                m_txLog.add("Response to RCPT for address \"" +
                            getAddress() + "\" was " + resp.getCode());
                getTransaction().toResponse(getAddress(), (resp.getCode() < 300));
                super.handleResponse(resp, actions);
            }
        }

    }//ENDOF BufferingTransactionHandler Class Definition

}//ENDOF BufferingSessionHandler Class Definition
