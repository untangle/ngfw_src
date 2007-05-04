/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.mail.papi.smtp;

import static com.untangle.tran.util.Ascii.CRLF;

import java.util.ArrayList;
import java.util.List;

import com.untangle.mvvm.tran.TemplateValues;
import com.untangle.tran.mime.EmailAddress;


/**
 * Class representing an SmtpTransaction.  Maintains
 * the state of the transaction (see the enum, which
 * I don't know how to reference with JavaDoc)
 * as well as any TO/FROM EmailAddresses.
 * <br>
 * Since there is some gray area in the "middle"
 * between client and server, this class maintains
 * envelope data (recipients and sender)
 * as "provisional" until they are "confirmed".
 * Confirmed means the server accepted, provisional
 * means the request was issued by client yet final
 * disposition is unknown.
 * <br>
 * <br>
 * This class also implements {@link com.untangle.tran.util.TemplateValues TemplateValues}.
 * Valid key names which can be derefferenced from an SmtpTransaction begin with
 * the literal <code>$SMTPTransaction:</code> followed by any of the following tokens:
 * <ul>
 *   <li>
 *     <code><b>TO</b></code> The "TO" addresses, as passed in any "RCPT TO:" commands.
 *     Each recipient is on its own line.  Even if there is only one recipient,
 *     this variable will be substituted with the value of the recipient <i>followed
 *     by a CRLF.</i>  If there are no recipients (?!?), than this will
 *     simply return a blank String ("").
 *   </li>
 *   <li>
 *     <code><b>FROM</b></code> The "FROM" as passed in the "MAIL FROM:" command.
 *     If there is no FROM value (or it was "&lt;>"), the literal "&lt;>" will be substituted.
 *   </li>
 * </ul>
 */
public final class SmtpTransaction
    implements TemplateValues {

    private static final String SMTP_TX_TEMPLATE_PREFIX = "SMTPTransaction:".toLowerCase();
    private static final String TO_TV = "TO".toLowerCase();
    private static final String FROM_TV = "FROM".toLowerCase();

    /**
     * Enum of Transaction states.
     */
    public enum TransactionState {
        OPEN,
        COMMITTED,
        RESET,
        FAILED
    }

    private TransactionState m_state = TransactionState.OPEN;
    private EmailAddress m_from;
    private boolean m_fromConfirmed;
    private List<EmailAddressWithStatus> m_recipients;
    private boolean m_hasAtLeastOneRecipient = false;

    public SmtpTransaction() {
        m_recipients = new ArrayList<EmailAddressWithStatus>();
        m_from = null;
        m_fromConfirmed = false;
    }

    /**
     * Access the state of the transaction
     */
    public TransactionState getState() {
        return m_state;
    }

    /**
     * Change the state to "COMITTED"
     */
    public void commit() {
        m_state = TransactionState.COMMITTED;
    }

    /**
     * Change the state to "RESET"
     */
    public void reset() {
        m_state = TransactionState.RESET;
    }

    /**
     * Change the state to "FAILED"
     */
    public void failed() {
        m_state = TransactionState.FAILED;
    }

    /**
     * Test if this transaction is still open.
     */
    public boolean isOpen() {
        return m_state == TransactionState.OPEN;
    }

    /**
     * Get the recipients ("RCPT TO...") for this
     * Transaction.
     *
     * @param confirmedOnly if true, only those recipients
     *        who have been positivly acknowledged by the
     *        server are returned.
     *
     * @return the recipients
     */
    public List<EmailAddress> getRecipients(boolean confirmedOnly) {
        List<EmailAddress> ret = new ArrayList<EmailAddress>();
        for(EmailAddressWithStatus eaws : m_recipients) {
            if((confirmedOnly && eaws.confirmed) ||
               (!confirmedOnly)) {
                ret.add(eaws.addr);
            }
        }
        return ret;
    }

    /**
     * A recipient has been requested (an "RCPT TO...")
     * issued.  Queue the recipient provisionally.
     */
    public void toRequest(EmailAddress addr) {
        m_recipients.add(new EmailAddressWithStatus(addr));
    }

    /**
     * The server has responded to a
     * previous RCPT TO... request.  The Transaction
     * should change its internal recipient
     * collection accordingly
     *
     * @param accept if true, the server accepted
     *        the recipient.
     */
    public void toResponse(EmailAddress addr,
                           boolean accept) {
        //In case someone (dumb) attempts
        //to request the same recipient twice,
        //scan from top-down for the to
        m_hasAtLeastOneRecipient = true;

        for(int i = 0; i<m_recipients.size(); i++) {
            EmailAddressWithStatus eaws = m_recipients.get(i);
            if(!eaws.addr.equals(addr)) {
                continue;
            }
            if(accept) {
                eaws.confirmed = true;
            }
            else {
                m_recipients.remove(i);
            }
            return;
        }
        //If we're here, there is a programming
        //error with the caller
        //TODO bscott assert?  Warn?
        EmailAddressWithStatus eaws = new EmailAddressWithStatus(addr);
        eaws.confirmed = true;
        m_recipients.add(eaws);
    }

    /**
     * Quick test to see if there is at least one
     * {@link #toResponse confirmed recipient}.
     *
     * @return true if at least one recipient has been confirmed.
     */
    public boolean hasAtLeastOneConfirmedRecipient() {
        return m_hasAtLeastOneRecipient;
    }

    /**
     * The client has issued a "MAIL FROM..." command.
     * The transaction will record this address as
     * the FROM provisionally.
     */
    public void fromRequest(EmailAddress addr) {
        m_from = addr;
        m_fromConfirmed = false;
    }

    /**
     * Change the internal envelope data to reflect
     * the server's response to the "MAIL" command
     *
     * @param accept did the server accept the address.
     */
    public void fromResponse(EmailAddress addr,
                             boolean accept) {
        if(m_from == null) {
            //TODO bscott programming error
            m_from = null;
            return;
        }
        if(accept) {
            m_fromConfirmed = true;
        }
        else {
            m_from = null;
            m_fromConfirmed = false;
        }
    }


    /**
     * Get the FROM for the envelope.  May
     * be null.  To test if this has
     * been confirmed, use {@link #isFromConfirmed}
     */
    public EmailAddress getFrom() {
        return m_from;
    }

    /**
     * Test if the FROM has been confirmed by the
     * server.
     */
    public boolean isFromConfirmed() {
        return m_fromConfirmed;
    }

    /**
     * For use in Templates (see JavaDoc at the top of this class
     * for explanation of vairable format}.
     */
    public String getTemplateValue(String key) {
        key = key.trim().toLowerCase();
        if(key.startsWith(SMTP_TX_TEMPLATE_PREFIX)) {
            key = key.substring(SMTP_TX_TEMPLATE_PREFIX.length());
            if(key.equals(TO_TV)) {
                StringBuilder sb = new StringBuilder();
                for(EmailAddressWithStatus eaws : m_recipients) {
                    sb.append(eaws.addr.toSMTPString());
                    sb.append(CRLF);
                }
                return sb.toString();
            }
            else if(key.equals(FROM_TV)) {
                return (m_from==null || m_from.isNullAddress())?
                    "<>":m_from.toSMTPString();
            }
        }
        return null;
    }



    private class EmailAddressWithStatus {
        final EmailAddress addr;
        boolean confirmed;

        EmailAddressWithStatus(EmailAddress addr) {
            this.addr = addr;
            this.confirmed = false;
        }
    }

}
