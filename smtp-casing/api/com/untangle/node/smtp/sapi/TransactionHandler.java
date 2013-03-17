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

import com.untangle.node.smtp.BeginMIMEToken;
import com.untangle.node.smtp.CompleteMIMEToken;
import com.untangle.node.smtp.ContinuedMIMEToken;
import com.untangle.node.smtp.Command;
import com.untangle.node.smtp.MAILCommand;
import com.untangle.node.smtp.RCPTCommand;
import com.untangle.node.smtp.SmtpTransaction;


/**
 * Class which handles Commands/MIMEBits
 * within the scope of an Smtp Transaction.
 * Most of the interesting documentation
 * is on
 * {@link com.untangle.node.smtp.sapi.SessionHandler SessionHandler}.
 * <br>
 * Note that the TransactionHandler is responsible
 * for manipulating the state/members of the
 * {@link #getTransaction associated transaction}.
 * In other words, if it was ambigious the caller
 * of this class does <b>not</b> change the
 * {@link #getTransaction associated transaction} based on
 * the flow of requests/responses.
 *
 */
public abstract class TransactionHandler {


    private final SmtpTransaction m_tx;

    /**
     * Construct a new TransactionHandler,
     * associated with the given Transaction.
     */
    public TransactionHandler(SmtpTransaction tx) {
        m_tx = tx;
    }
    /**
     * Access the associated Transaction.
     *
     * @return the transaction
     */
    public final SmtpTransaction getTransaction() {
        return m_tx;
    }

    /**
     * Handle an RSET.  This marks the end of the transaction,
     * although the RSET may still be issued to the server
     * (and the response received).
     *
     * @param command the command
     * @param actions the available actions
     */
    public abstract void handleRSETCommand(Command command,
                                           Session.SmtpCommandActions actions);

    /**
     * Handle a "normal" SMTP Command (non-transaction impacting)
     * received during a Transaction.
     *
     * @param command the command
     * @param actions the available actions
     */
    public abstract void handleCommand(Command command,
                                       Session.SmtpCommandActions actions);

    /**
     * Handle a MAIL Command.
     *
     * @param command the command
     * @param actions the available actions
     */
    public abstract void handleMAILCommand(MAILCommand command,
                                           Session.SmtpCommandActions action);

    /**
     * Handle a RCPT Command.
     *
     * @param command the command
     * @param actions the available actions
     */
    public abstract void handleRCPTCommand(RCPTCommand command,
                                           Session.SmtpCommandActions action);

    /**
     * Handle a BeginMIMEToken
     *
     * @param token the token
     * @param actions the available actions
     */
    public abstract void handleBeginMIME(BeginMIMEToken token,
                                         Session.SmtpCommandActions action);

    /**
     * Handle a ContinuedMIMEToken
     *
     * @param token the token
     * @param actions the available actions
     */
    public abstract void handleContinuedMIME(ContinuedMIMEToken token,
                                             Session.SmtpCommandActions action);

    /**
     * Handle a Complete MIME token
     *
     * @param token the token
     * @param actions the available actions
     */
    public abstract void handleCompleteMIME(CompleteMIMEToken token,
                                            Session.SmtpCommandActions action);

    /**
     * Called when both client and server sides are closed.  Any
     * associated resources should be closed and any interesting logging
     * made.
     * <br><br>
     * Will only be called if there is an open transaction at
     * completion time.
     */
    public abstract void handleFinalized();

}
