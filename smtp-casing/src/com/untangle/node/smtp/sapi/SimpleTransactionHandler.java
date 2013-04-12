/**
 * $Id$
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
import com.untangle.node.smtp.mime.EmailAddress;

/**
 * Implementation of TransactionHandler which
 * does nothing except pass everything along
 * and perform some debug logging.
 */
public class SimpleTransactionHandler extends TransactionHandler
{
    private final Logger m_logger = Logger.getLogger(SimpleTransactionHandler.class);

    public SimpleTransactionHandler(SmtpTransaction tx)
    {
        super(tx);
    }

    @Override
    public void handleRSETCommand(Command command, Session.SmtpCommandActions actions)
    {

        m_logger.debug("[handleRSETCommand] (will pass along to handleCommand)");
        getTransaction().reset();
        actions.transactionEnded(this);
        handleCommand(command, actions);
    }

    @Override
    public void handleCommand(Command command, Session.SmtpCommandActions actions)
    {

        m_logger.debug("[handleCommand]");
        actions.sendCommandToServer(command, new PassthruResponseCompletion());
    }

    @Override
    public void handleMAILCommand(MAILCommand command, Session.SmtpCommandActions actions)
    {

        m_logger.debug("[handleMAILCommand]");
        actions.sendCommandToServer(command, new MAILContinuation(command.getAddress()));
    }

    @Override
    public void handleRCPTCommand(RCPTCommand command, Session.SmtpCommandActions actions)
    {

        m_logger.debug("[handleRCPTCommand]");
        actions.sendCommandToServer(command, new RCPTContinuation(command.getAddress()));
    }

    @Override
    public void handleBeginMIME(BeginMIMEToken token, Session.SmtpCommandActions actions)
    {
        m_logger.debug("[handleBeginMIME] (no response expected, so none will be queued");
        actions.sendBeginMIMEToServer(token);
    }

    @Override
    public void handleContinuedMIME(ContinuedMIMEToken token, Session.SmtpCommandActions actions)
    {
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
    public void handleCompleteMIME(CompleteMIMEToken token, Session.SmtpCommandActions actions)
    {
        m_logger.debug("[handleCompleteMIME]");
        actions.sentWholeMIMEToServer(token, new DataTransmissionContinuation());
    }

    @Override
    public void handleFinalized()
    {
        //
    }

    private class DataTransmissionContinuation
        extends PassthruResponseCompletion
    {

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

    private class ContinuationWithAddress
        extends PassthruResponseCompletion
    {

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

    private class MAILContinuation
        extends ContinuationWithAddress
    {

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

    private class RCPTContinuation
        extends ContinuationWithAddress
    {

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
