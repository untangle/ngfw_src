/**
 * $Id$
 */
package com.untangle.node.smtp.sapi;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.Command;
import com.untangle.node.smtp.SmtpTransaction;

/**
 * Implementation of SessionHandler which
 * does nothing except pass everything along
 * and perform some debug logging.
 */
public class SimpleSessionHandler extends SessionHandler
{

    private final Logger m_logger = Logger.getLogger(SimpleSessionHandler.class);

    public void handleCommand(Command command, Session.SmtpCommandActions actions)
    {
        m_logger.debug("[handleCommand] with command of type \"" +
                       command.getType() + "\"");
        actions.sendCommandToServer(command, new PassthruResponseCompletion());

    }

    public TransactionHandler createTxHandler(SmtpTransaction tx)
    {
        return new SimpleTransactionHandler(tx);
    }

    public boolean handleServerFIN(TransactionHandler currentTX)
    {
        return true;
    }

    public boolean handleClientFIN(TransactionHandler currentTX)
    {
        return true;
    }

    @Override
    public void handleFinalized()
    {
        //
    }

}
