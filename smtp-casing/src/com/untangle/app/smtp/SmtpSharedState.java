/**
 * $Id: SmtpSharedState.java 36445 2013-11-20 00:04:22Z dmorris $
 */
package com.untangle.app.smtp;

import java.util.LinkedList;
import java.util.List;

import javax.mail.internet.InternetAddress;

import com.untangle.app.smtp.sasl.SASLObserver;
import com.untangle.app.smtp.sasl.SASLObserverFactory;

import org.apache.log4j.Logger;


/**
 * Class which is shared between Client Parser and Unparser, observing state transitions (esp: transaction-impacting)
 * Commands and Responses to accumulate who is part of a transaction (TO/FROM) and to align requests with responses.
 */
class SmtpSharedState
{

    /**
     * Interface for Object wishing to be called-back when the response to a given Command is received.
     */
    interface ResponseAction
    {
        /**
         * Callback corresponding to the Command for-which this action was registered. <br>
         * <br>
         * Note that any changes to the internal state of the Tracker have <b>already</b> been made (i.e. the tracker
         * sees the response before the callback).
         * @param code reponse to use.
         */
        void response(int code);
    }

    /**
     * Holds the last few commands
     */
    private class CSHistory
    {
        private final String[] m_items;
        private final int m_len;
        private int m_tail = 0;
        private int m_head = 0;

        /**
         * Initialize instance with length.
         * @param len Lenth of history to retain.
         */
        CSHistory(int len)
        {
            m_items = new String[len + 1];
            m_len = len + 1;
        }

        /**
         * Add command to history.
         * @param str SMTP command to add.
         */
        void add(String str)
        {

            int nextTail = next(m_tail);
            if (nextTail == m_head) {
                m_head = next(m_head);
            }
            m_items[m_tail] = str;
            m_tail = nextTail;
        }

        /**
         * Return List of history.
         * @return String list of history.
         */
        java.util.List<String> getHistory()
        {
            java.util.List<String> ret = new java.util.ArrayList<>();

            int head = m_head;

            while (head != m_tail) {
                ret.add(m_items[head]);
                head = next(head);
            }
            return ret;
        }

        /**
         * Move to next position.
         * @param  i Amount to move.
         * @return   Current position.
         */
        private int next(int i)
        {
            return (++i >= m_len) ? 0 : i;
        }

    }

    private static final long LIKELY_TIMEOUT_LENGTH = 1000 * 60;// 1 minute

    private final Logger logger = Logger.getLogger(SmtpSharedState.class);

    protected SmtpTransaction currentTransaction;
    protected List<ResponseAction> outstandingRequests;
    protected CSHistory history = new CSHistory(25);
    protected long lastTransmissionTimestamp;
    protected boolean passthru = false;
    protected SmtpSASLObserver saslObserver;
    
    /**
     * SMTP Shared state.
     * @return instance of SmtpSharedState
     */
    public SmtpSharedState()
    {
        outstandingRequests = new LinkedList<>();
        // Add response for initial salutation
        outstandingRequests.add(new SimpleResponseAction());
        updateLastTransmissionTimestamp();
    }

    /**
     * Get the underlying transaction. May be null if this tracker thinks there is no outstanding transaction.
     * @return current transation.
     */
    SmtpTransaction getCurrentTransaction()
    {
        return currentTransaction;
    }

    /**
     * Begin message transmission.
     */
    void beginMsgTransmission()
    {
        beginMsgTransmission(null);
    }

    /**
     * Begin message transmission with a chained action.
     * @param chainedAction Response action to add.
     */
    void beginMsgTransmission(ResponseAction chainedAction)
    {
        getOrCreateTransaction();
        outstandingRequests.add(new TransmissionResponseAction(chainedAction));
        history.add("(c) <Begin Msg Transmission>");

    }

    /**
     * Inform that the server has been shut-down. This enqueues an extra response handler (in case the server ACKS the
     * FIN).
     */
    void serverShutdown()
    {
        outstandingRequests.add(new SimpleResponseAction());
    }

    /**
     * Initialize command received.
     * @param command Command to use.
     */
    void commandReceived(Command command)
    {
        commandReceived(command, null);
    }

    /**
     * Initialize command received with a chained action.
     * @param command Command to use.
     * @param chainedAction ResponseAction to add.
     */
    void commandReceived(Command command, ResponseAction chainedAction)
    {
        logger.debug( "Command received: " + command.getCmdString() );

        if (command instanceof UnparsableCommand) {
            history.add("(c) " + command.getCmdString() + " (" + command.getArgString() + ")");
        } else {
            history.add("(c) " + command.getCmdString());
        }

        ResponseAction action = null;
        if (command.getType() == CommandType.MAIL) {
            InternetAddress addr = ((CommandWithEmailAddress) command).getAddress();
            getOrCreateTransaction().fromRequest(addr);
            action = new MAILResponseAction(addr, chainedAction);
        } else if (command.getType() == CommandType.RCPT) {
            InternetAddress addr = ((CommandWithEmailAddress) command).getAddress();
            getOrCreateTransaction().toRequest(addr);
            action = new RCPTResponseAction(addr, chainedAction);
        } else if (command.getType() == CommandType.RSET) {
            getOrCreateTransaction().reset();
            currentTransaction = null;
            action = new SimpleResponseAction(chainedAction);
        } else if (command.getType() == CommandType.DATA) {
            action = new DATAResponseAction(chainedAction);
        } else {
            action = new SimpleResponseAction(chainedAction);
        }
        outstandingRequests.add(action);
    }

    /**
     * Add response.
     * @param response Response to add.
     */
    void responseReceived(Response response)
    {
        history.add("(s) " + response.getCode());

        logger.debug( "Response received: " + response.getCode() );
        if (outstandingRequests.size() == 0) {
            long diff = System.currentTimeMillis() - lastTransmissionTimestamp;
            if (diff > LIKELY_TIMEOUT_LENGTH) {
                logger.info("Unsolicited response from server.  Likely a timeout notification as " + diff + " milliseconds have transpired since last communication");
            } else {
                logger.warn("No outstanding request for response: " + response.getCode() + " Recent history: " + historyToString());
            }
        } else {
            outstandingRequests.remove(0).response(response.getCode());
        }
    }

    /**
     * Set last transmisison timestamp to currrent time.
     */
    private void updateLastTransmissionTimestamp()
    {
        lastTransmissionTimestamp = System.currentTimeMillis();
    }

    /**
     * Return the current transaction.  If there is now, create new one.
     * @return SmtpTransaction of current transaction.
     */
    private SmtpTransaction getOrCreateTransaction()
    {
        if (currentTransaction == null) {
            currentTransaction = new SmtpTransaction();
        }
        return currentTransaction;
    }

    /**
     * Return history as a string.
     * @return String of history.
     */
    private String historyToString()
    {
        StringBuilder sb = new StringBuilder();
        for (String s : history.getHistory()) {
            if (sb.length() != 0) {
                sb.append(',');
            }
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Chained response action.
     */
    private abstract class ChainedResponseAction implements ResponseAction
    {

        private final ResponseAction m_chained;

        /**
         * Initialize instance of ChainedResponseAction.
         * @return instance of ChainedResponseAction
         */
        ChainedResponseAction() {
            this(null);
        }

        /**
         * Initialize instance of ChainedResponseAction.
         * @param chained chained value to initialize.
         * @return instance of ChainedResponseAction
         */
        ChainedResponseAction(ResponseAction chained) {
            m_chained = chained;
        }

        /**
         * Set response with code.
         * @param code Code to set.
         */
        public final void response(int code)
        {
            responseImpl(code);
            if (m_chained != null) {
                m_chained.response(code);
            }
        }

        /**
         * Abstract of response implementation.
         * @param code Code to process.
         */
        abstract void responseImpl(int code);
    }

    /**
     * Simple Response action.
     */
    private class SimpleResponseAction extends ChainedResponseAction
    {

        /**
         * Initialize instance of SimpleResponseAction.
         * @return instance of SimpleResponseAction.
         */
        SimpleResponseAction() {
            super();
        }

        /**
         * Initialize instance of SimpleResponseAction.
         * @param chained ResponseAction indicating chained.
         */
        SimpleResponseAction(ResponseAction chained) {
            super(chained);
        }

        /**
         * Response implementation.
         * @param code SMTP code.
         */
        void responseImpl(int code)
        {
            // Do nothing ourselves
        }
    }

    /**
     * Mail Response action.
     */
    private class MAILResponseAction extends ChainedResponseAction
    {

        private final InternetAddress m_addr;

        /**
         * Initialize instance of MAILResponseAction.
         * @param addr InternetAddress IP address.
         * @param chained ResponseAction.
         * @return instance of MAILResponseAction.
         */
        MAILResponseAction(InternetAddress addr, ResponseAction chained) {
            super(chained);
            m_addr = addr;
        }

        /**
         * Response implementation.
         * @param code SMTP code.
         */
        void responseImpl(int code)
        {
            if (currentTransaction != null) {
                currentTransaction.fromResponse(m_addr, code < 300);
            }
        }
    }

    /**
     * RCPT response action.
     */
    private class RCPTResponseAction extends ChainedResponseAction
    {

        private final InternetAddress m_addr;

        /**
         * Initialize instance of RCPTResponseAction.
         * @param addr InternetAddress IP address.
         * @param chained ResponseAction.
         * @return instance of RCPTResponseAction.
         */
        RCPTResponseAction(InternetAddress addr, ResponseAction chained) {
            super(chained);
            m_addr = addr;
        }

        /**
         * Response implementation.
         * @param code SMTP code.
         */
        void responseImpl(int code)
        {
            if (currentTransaction != null) {
                currentTransaction.toResponse(m_addr, code < 300);
            }
        }
    }

    /**
     * DATA response action.
     */
    private class DATAResponseAction extends ChainedResponseAction
    {

        /**
         * Initialize instance of DATAResponseAction.
         * @param chained ResponseAction to initialize with.
         * @return instance of DATAResponseAction.
         */
        DATAResponseAction(ResponseAction chained) {
            super(chained);
        }

        /**
         * Response implementation.
         * @param code SMTP code.
         */
        void responseImpl(int code)
        {
            if (code >= 400) {
                getOrCreateTransaction().failed();
                currentTransaction = null;
            }
        }
    }

    /**
     * Transmission response action.
     */
    private class TransmissionResponseAction extends ChainedResponseAction
    {

        /**
         * Initialize instance of TransmissionResponseAction.
         * @param chained ResponseAction to initialize with.
         * @return instance of TransmissionResponseAction.
         */
        TransmissionResponseAction(ResponseAction chained) {
            super(chained);
        }

        /**
         * Response implementation.
         * @param code SMTP code.
         */
        void responseImpl(int code)
        {
            if (currentTransaction != null) {
                if (code < 300) {
                    currentTransaction.commit();
                } else {
                    currentTransaction.failed();
                }
            }
            currentTransaction = null;
        }
    }

    /**
     * Test if this session is engaged in a SASL exchange
     * 
     * @return true if in SASL login
     */
    boolean isInSASLLogin()
    {
        return saslObserver != null;
    }

    /**
     * Open a SASL exchange observer, based on the given mechanism name. If null is returned, a suitable SASLObserver
     * could not be found for the named mechanism (and we should punt on this session).
     * @param mechanismName String of mechanism.
     * @return true if able to create SASLObserverFactory, false otherwise.
     */
    boolean openSASLExchange(String mechanismName)
    {
        SASLObserver observer = SASLObserverFactory.createObserverForMechanism(mechanismName);
        if (observer == null) {
            logger.debug("Could not find SASLObserver for mechanism \"" + mechanismName + "\"");
            return false;
        }
        saslObserver = new SmtpSASLObserver(observer);
        return true;
    }

    /**
     * Get the current SASLObserver. If this returns null yet the caller thinks there is an open SASL exchange, this is
     * an error
     * 
     * @return the SmtpSASLObserver
     */
    SmtpSASLObserver getSASLObserver()
    {
        return saslObserver;
    }

    /**
     * Close the current SASLExchange
     */
    void closeSASLExchange()
    {
        saslObserver = null;
    }
    
}
