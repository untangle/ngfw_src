/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.smtp;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.Command;
import com.untangle.node.smtp.MAILCommand;
import com.untangle.node.smtp.RCPTCommand;
import com.untangle.node.smtp.Response;
import com.untangle.node.smtp.SmtpTransaction;
import com.untangle.node.smtp.UnparsableCommand;
import com.untangle.node.mime.EmailAddress;


/**
 * Class which is shared between Client Parser and Unparser, observing
 * state transitions (esp: transaction-impacting) Commands and
 * Responses to accumulate who is part of a transaction (TO/FROM)
 * and to align requests with responses.
 */
class CasingSessionTracker {

    /**
     * Interface for Object wishing to
     * be called-back when the response
     * to a given Command is received.
     */
    interface ResponseAction {
        /**
         * Callback corresponding to the
         * Command for-which this action
         * was registered.
         * <br><br>
         * Note that any changes to the internal
         * state of the Tracker have <b>already</b>
         * been made (i.e. the tracker sees the
         * response before the callback).
         */
        void response(int code);
    }

    /**
     * Holds the last few commands
     */
    private class CSHistory {
        private final String[] m_items;
        private final int m_len;
        private int m_tail = 0;
        private int m_head = 0;

        CSHistory(int len) {
            m_items = new String[len+1];
            m_len = len+1;
        }

        void add(String str) {

            int nextTail = next(m_tail);
            if(nextTail == m_head) {
                m_head = next(m_head);
            }
            m_items[m_tail] = str;
            m_tail = nextTail;
        }

        java.util.List<String> getHistory() {
            java.util.List<String> ret = new java.util.ArrayList<String>();

            int head = m_head;

            while(head != m_tail) {
                ret.add(m_items[head]);
                head = next(head);
            }
            return ret;
        }

        private int next(int i) {
            return (++i >= m_len)?0:i;
        }

    }


    private static final long LIKELY_TIMEOUT_LENGTH = 1000*60;//1 minute

    private final Logger m_logger = Logger.getLogger(CasingSessionTracker.class);

    private SmtpTransaction m_currentTransaction;
    private List<ResponseAction> m_outstandingRequests;
    private boolean m_closing = false;
    private CSHistory m_history = new CSHistory(25);
    private long m_lastTransmissionTimestamp;



    CasingSessionTracker() {
        m_outstandingRequests = new LinkedList<ResponseAction>();
        //Add response for initial salutation
        m_outstandingRequests.add(new SimpleResponseAction());
        updateLastTransmissionTimestamp();
    }

    /**
     * Get the underlying transaction.  May be null if
     * this tracker thinks there is no outstanding transaction.
     */
    SmtpTransaction getCurrentTransaction() {
        return m_currentTransaction;
    }
    /**
     * Inform the tracker that we're closing, so it can supress
     * any final message from the server.
     */
    void closing() {
        m_closing = true;
    }

    void beginMsgTransmission() {
        beginMsgTransmission(null);
    }

    void beginMsgTransmission(ResponseAction chainedAction) {
        getOrCreateTransaction();
        m_outstandingRequests.add(new TransmissionResponseAction(chainedAction));
        m_history.add("(c) <Begin Msg Transmission>");

    }
    /**
     * Inform that the server has been shut-down.  This
     * enqueues an extra response handler (in case the server
     * ACKS the FIN).
     */
    void serverShutdown() {
        m_outstandingRequests.add(new SimpleResponseAction());
    }

    void commandReceived(Command command) {
        commandReceived(command, null);
    }

    void commandReceived(Command command,
                         ResponseAction chainedAction) {

        if(command instanceof UnparsableCommand) {
            m_history.add("(c) " + command.getCmdString() + " (" +
                          command.getArgString() + ")");
        }
        else {
            m_history.add("(c) " + command.getCmdString());
        }


        ResponseAction action = null;
        if(command.getType() == Command.CommandType.MAIL) {
            EmailAddress addr = ((MAILCommand) command).getAddress();
            getOrCreateTransaction().fromRequest(addr);
            action = new MAILResponseAction(addr, chainedAction);
        }
        else if(command.getType() == Command.CommandType.RCPT) {
            EmailAddress addr = ((RCPTCommand) command).getAddress();
            getOrCreateTransaction().toRequest(addr);
            action = new RCPTResponseAction(addr, chainedAction);
        }
        else if(command.getType() == Command.CommandType.RSET) {
            getOrCreateTransaction().reset();
            m_currentTransaction = null;
            action = new SimpleResponseAction(chainedAction);
        }
        else if(command.getType() == Command.CommandType.DATA) {
            action = new DATAResponseAction(chainedAction);
        }
        else {
            action = new SimpleResponseAction(chainedAction);
        }
        m_outstandingRequests.add(action);
    }


    void responseReceived(Response response) {
        m_history.add("(s) " + response.getCode());
        if(m_outstandingRequests.size() == 0) {
            if(!m_closing) {
                long diff = System.currentTimeMillis() - m_lastTransmissionTimestamp;
                if(diff > LIKELY_TIMEOUT_LENGTH) {
                    m_logger.info("Unsolicited response from server.  Likely a timeout notification as " +
                                  diff + " milliseconds have transpired since last communication");
                }
                else {
                    m_logger.warn("Misalignment of req/resp tracking.  No outstanding response.  " +
                                  "Recent history: " + historyToString());
                }
            }
        }
        else {
            m_outstandingRequests.remove(0).response(response.getCode());
        }
    }

    private void updateLastTransmissionTimestamp() {
        m_lastTransmissionTimestamp = System.currentTimeMillis();
    }

    private SmtpTransaction getOrCreateTransaction() {
        if(m_currentTransaction == null) {
            m_currentTransaction = new SmtpTransaction();
        }
        return m_currentTransaction;
    }

    private String historyToString() {
        StringBuilder sb = new StringBuilder();
        for(String s : m_history.getHistory()) {
            if(sb.length() != 0) {
                sb.append(',');
            }
            sb.append(s);
        }
        return sb.toString();
    }

    private abstract class ChainedResponseAction
        implements ResponseAction {

        private final ResponseAction m_chained;

        ChainedResponseAction() {
            this(null);
        }
        ChainedResponseAction(ResponseAction chained) {
            m_chained = chained;
        }

        public final void response(int code) {
            responseImpl(code);
            if(m_chained != null) {
                m_chained.response(code);
            }
        }

        abstract void responseImpl(int code);
    }

    private class SimpleResponseAction
        extends ChainedResponseAction {

        SimpleResponseAction() {
            super();
        }
        SimpleResponseAction(ResponseAction chained) {
            super(chained);
        }

        void responseImpl(int code) {
            //Do nothing ourselves
        }
    }


    private class MAILResponseAction
        extends ChainedResponseAction {

        private final EmailAddress m_addr;

        MAILResponseAction(EmailAddress addr,
                           ResponseAction chained) {
            super(chained);
            m_addr = addr;
        }

        void responseImpl(int code) {
            if(m_currentTransaction != null) {
                m_currentTransaction.fromResponse(m_addr, code<300);
            }
        }
    }


    private class RCPTResponseAction
        extends ChainedResponseAction {

        private final EmailAddress m_addr;

        RCPTResponseAction(EmailAddress addr,
                           ResponseAction chained) {
            super(chained);
            m_addr = addr;
        }

        void responseImpl(int code) {
            if(m_currentTransaction != null) {
                m_currentTransaction.toResponse(m_addr, code<300);
            }
        }
    }

    private class DATAResponseAction
        extends ChainedResponseAction {

        DATAResponseAction(ResponseAction chained) {
            super(chained);
        }

        void responseImpl(int code) {
            if(code >= 400) {
                getOrCreateTransaction().failed();
                m_currentTransaction = null;
            }
        }
    }

    private class TransmissionResponseAction
        extends ChainedResponseAction {

        TransmissionResponseAction(ResponseAction chained) {
            super(chained);
        }

        void responseImpl(int code) {
            if(m_currentTransaction != null) {
                if(code < 300) {
                    m_currentTransaction.commit();
                }
                else {
                    m_currentTransaction.failed();
                }
            }
            m_currentTransaction = null;
        }
    }

}
