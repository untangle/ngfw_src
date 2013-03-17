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

/**
 * Implementation of SessionHandler which
 * is used to behave like a server which wants
 * to shut-down the connection with the
 * client, but is well-bahaved enough
 * to keep the connection open for a
 * bit to let the client know this.
 */
public class ShuttingDownSessionHandler
    extends SessionHandler {

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
