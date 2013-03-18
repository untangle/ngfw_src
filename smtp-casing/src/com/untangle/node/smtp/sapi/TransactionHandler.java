/**
 * $Id$
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
public abstract class TransactionHandler
{
    private final SmtpTransaction m_tx;

    /**
     * Construct a new TransactionHandler,
     * associated with the given Transaction.
     */
    public TransactionHandler(SmtpTransaction tx)
    {
        m_tx = tx;
    }

    /**
     * Access the associated Transaction.
     *
     * @return the transaction
     */
    public final SmtpTransaction getTransaction()
    {
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
    public abstract void handleRSETCommand(Command command, Session.SmtpCommandActions actions);

    /**
     * Handle a "normal" SMTP Command (non-transaction impacting)
     * received during a Transaction.
     *
     * @param command the command
     * @param actions the available actions
     */
    public abstract void handleCommand(Command command, Session.SmtpCommandActions actions);

    /**
     * Handle a MAIL Command.
     *
     * @param command the command
     * @param actions the available actions
     */
    public abstract void handleMAILCommand(MAILCommand command, Session.SmtpCommandActions action);

    /**
     * Handle a RCPT Command.
     *
     * @param command the command
     * @param actions the available actions
     */
    public abstract void handleRCPTCommand(RCPTCommand command, Session.SmtpCommandActions action);

    /**
     * Handle a BeginMIMEToken
     *
     * @param token the token
     * @param actions the available actions
     */
    public abstract void handleBeginMIME(BeginMIMEToken token, Session.SmtpCommandActions action);

    /**
     * Handle a ContinuedMIMEToken
     *
     * @param token the token
     * @param actions the available actions
     */
    public abstract void handleContinuedMIME(ContinuedMIMEToken token, Session.SmtpCommandActions action);

    /**
     * Handle a Complete MIME token
     *
     * @param token the token
     * @param actions the available actions
     */
    public abstract void handleCompleteMIME(CompleteMIMEToken token, Session.SmtpCommandActions action);

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
