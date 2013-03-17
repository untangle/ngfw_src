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

/**
 * Implementation of SessionHandler which
 * is used to behave like a server which wants
 * to shut-down the connection with the
 * client, but is well-bahaved enough
 * to keep the connection open for a
 * bit to let the client know this.
 */
public class ShuttingDownSessionHandler extends SessionHandler
{
    private final Logger m_logger = Logger.getLogger(ShuttingDownSessionHandler.class);

    //Time (absolute) when the class should stop being
    //nice, and nuke the connection with the client
    private long m_quitAt;

    /**
     * Construct a new ShuttingDownSessionHandler
     *
     * @param maxTime the maximum time this handler should
     *        entertain commands from the client, before
     *        simply chutting down the connection.
     */
    public ShuttingDownSessionHandler(long maxTime) {
        m_quitAt = System.currentTimeMillis() + maxTime;
    }

    @Override
    public void handleCommand(Command command,
                              Session.SmtpCommandActions actions) {
        handleCommandImpl(command, actions);
    }

    @Override
    public void handleOpeningResponse(Response resp,
                                      Session.SmtpResponseActions actions) {
        //Isn't this impossible?!?
        //Check for our timeout
        if(timedOut(actions)) {
            return;
        }
        actions.sendResponseToClient(new Response(421,
                                                  "Service not available, closing transmission channel"));
    }

    @Override
    public TransactionHandler createTxHandler(SmtpTransaction tx) {
        return new ShuttingDownTransactionHandler(tx);
    }

    @Override
    public boolean handleServerFIN(TransactionHandler currentTX) {
        m_logger.debug("Supress Server FIN");
        return false;
    }

    @Override
    public boolean handleClientFIN(TransactionHandler currentTX) {
        return true;
    }

    @Override
    public void handleFinalized() {
        //
    }

    private void handleCommandImpl(Command command,
                                   Session.SmtpCommandActions actions) {

        //Check for our timeout
        if(timedOut(actions)) {
            return;
        }

        //Check for "special" commands
        if(command.getType() == Command.CommandType.QUIT) {
            actions.sendFINToClient();
            return;
        }
        if(command.getType() == Command.CommandType.RSET) {
            actions.sendResponseNow(new Response(250, "OK"));
            return;
        }
        if(command.getType() == Command.CommandType.NOOP) {
            actions.sendResponseNow(new Response(250, "OK"));
            return;
        }
        send421(actions);
    }

    private boolean timedOut(Session.SmtpActions actions) {
        if(System.currentTimeMillis() > m_quitAt) {
            actions.sendFINToClient();
            return true;
        }
        return false;
    }

    private void send421(Session.SmtpCommandActions actions) {
        actions.sendResponseNow(new Response(421,
                                             "Service not available, closing transmission channel"));
    }

    //================= Inner Class ======================

    class ShuttingDownTransactionHandler
        extends TransactionHandler {

        public ShuttingDownTransactionHandler(SmtpTransaction tx) {
            super(tx);
        }

        @Override
        public void handleRSETCommand(Command command,
                                      Session.SmtpCommandActions actions) {
            actions.transactionEnded(this);
            handleCommandImpl(command, actions);
        }

        @Override
        public void handleCommand(Command command,
                                  Session.SmtpCommandActions actions) {
            actions.transactionEnded(this);
            handleCommandImpl(command, actions);
        }
        @Override
        public void handleMAILCommand(MAILCommand command,
                                      Session.SmtpCommandActions actions) {
            actions.transactionEnded(this);
            handleCommandImpl(command, actions);
        }
        @Override
        public void handleRCPTCommand(RCPTCommand command,
                                      Session.SmtpCommandActions actions) {
            actions.transactionEnded(this);
            handleCommandImpl(command, actions);
        }
        @Override
        public void handleBeginMIME(BeginMIMEToken token,
                                    Session.SmtpCommandActions actions) {
            actions.transactionEnded(this);
            //Check for our timeout
            if(timedOut(actions)) {
                return;
            }
            send421(actions);
        }
        @Override
            public void handleContinuedMIME(ContinuedMIMEToken token,
                                            Session.SmtpCommandActions actions) {
            actions.transactionEnded(this);
            //Check for our timeout
            if(timedOut(actions)) {
                return;
            }
            send421(actions);
        }
        @Override
        public void handleCompleteMIME(CompleteMIMEToken token,
                                       Session.SmtpCommandActions actions) {
            actions.transactionEnded(this);
            //Check for our timeout
            if(timedOut(actions)) {
                return;
            }
            send421(actions);
        }
        @Override
        public void handleFinalized() {
            //
        }
    }







}
