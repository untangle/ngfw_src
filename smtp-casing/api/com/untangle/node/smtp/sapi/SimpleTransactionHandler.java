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

package com.untangle.node.smtp.sapi;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.BeginMIMEToken;
import com.untangle.node.smtp.CompleteMIMEToken;
import com.untangle.node.smtp.ContinuedMIMEToken;
import com.untangle.node.smtp.Command;
import com.untangle.node.smtp.MAILCommand;
import com.untangle.node.smtp.RCPTCommand;
import com.untangle.node.smtp.Response;
import com.untangle.node.smtp.SmtpTransaction;
import com.untangle.node.mime.EmailAddress;

/**
 * Implementation of TransactionHandler which
 * does nothing except pass everything along
 * and perform some debug logging.
 */
public class SimpleTransactionHandler
    extends TransactionHandler {

    private final Logger m_logger = Logger.getLogger(SimpleTransactionHandler.class);

    public SimpleTransactionHandler(SmtpTransaction tx) {
        super(tx);
    }

    @Override
    public void handleRSETCommand(Command command,
                                  Session.SmtpCommandActions actions) {

        m_logger.debug("[handleRSETCommand] (will pass along to handleCommand)");
        getTransaction().reset();
        actions.transactionEnded(this);
        handleCommand(command, actions);
    }

    @Override
    public void handleCommand(Command command,
                              Session.SmtpCommandActions actions) {

        m_logger.debug("[handleCommand]");
        actions.sendCommandToServer(command, new PassthruResponseCompletion());
    }
    @Override
    public void handleMAILCommand(MAILCommand command,
                                  Session.SmtpCommandActions actions) {

        m_logger.debug("[handleMAILCommand]");
        actions.sendCommandToServer(command, new MAILContinuation(command.getAddress()));
    }
    @Override
    public void handleRCPTCommand(RCPTCommand command,
                                  Session.SmtpCommandActions actions) {

        m_logger.debug("[handleRCPTCommand]");
        actions.sendCommandToServer(command, new RCPTContinuation(command.getAddress()));
    }
    @Override
    public void handleBeginMIME(BeginMIMEToken token,
                                Session.SmtpCommandActions actions) {
        m_logger.debug("[handleBeginMIME] (no response expected, so none will be queued");
        actions.sendBeginMIMEToServer(token);
    }
    @Override
        public void handleContinuedMIME(ContinuedMIMEToken token,
                                        Session.SmtpCommandActions actions) {
        if(token.isLast()) {
            m_logger.debug("[handleContinuedMIME] (last token, so enqueue a continuation for the response");
            actions.sendFinalMIMEToServer(token, new DataTransmissionContinuation());
        }
        else {
            m_logger.debug("[handleContinuedMIME] (not last - no response expected, so none will be queued");
            actions.sendContinuedMIMEToServer(token);
        }
    }
    @Override
    public void handleCompleteMIME(CompleteMIMEToken token,
                                   Session.SmtpCommandActions actions) {
        m_logger.debug("[handleCompleteMIME]");
        actions.sentWholeMIMEToServer(token, new DataTransmissionContinuation());
    }

    @Override
    public void handleFinalized() {
        //
    }





    //==========================
    // Inner-Classes
    //===========================


    //=============== Inner Class Separator ====================

    private class DataTransmissionContinuation
        extends PassthruResponseCompletion {

        public void handleResponse(Response resp,
                                   Session.SmtpResponseActions actions) {
            m_logger.debug("[$DataTransmissionContinuation][handleResponse]");
            if(resp.getCode() < 300) {
                getTransaction().commit();
            }
            else {
                getTransaction().failed();
            }
            actions.transactionEnded(SimpleTransactionHandler.this);
            super.handleResponse(resp, actions);
        }
    }



    //=============== Inner Class Separator ====================

    private class ContinuationWithAddress
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


    //=============== Inner Class Separator ====================

    private class MAILContinuation
        extends ContinuationWithAddress {

        MAILContinuation(EmailAddress addr) {
            super(addr);
        }

        public void handleResponse(Response resp,
                                   Session.SmtpResponseActions actions) {
            m_logger.debug("[$MAILContinuation][handleResponse]");
            getTransaction().fromResponse(getAddress(), (resp.getCode() < 300));
            super.handleResponse(resp, actions);
        }
    }


    //=============== Inner Class Separator ====================

    private class RCPTContinuation
        extends ContinuationWithAddress {

        RCPTContinuation(EmailAddress addr) {
            super(addr);
        }

        public void handleResponse(Response resp,
                                   Session.SmtpResponseActions actions) {
            m_logger.debug("[$RCPTContinuation][handleResponse]");
            getTransaction().toResponse(getAddress(), (resp.getCode() < 300));
            super.handleResponse(resp, actions);
        }
    }

}
