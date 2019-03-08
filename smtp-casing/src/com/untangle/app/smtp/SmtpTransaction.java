/**
 * $Id$
 */
package com.untangle.app.smtp;

import static com.untangle.uvm.util.Ascii.CRLF;

import java.util.ArrayList;
import java.util.List;

import javax.mail.internet.InternetAddress;

import org.apache.log4j.Logger;

import com.untangle.app.smtp.mime.MIMEUtil;

/**
 * Class representing an SmtpTransaction. Maintains the state of the transaction (see the enum, which I don't know how
 * to reference with JavaDoc) as well as any TO/FROM EmailAddresses. <br>
 * Since there is some gray area in the "middle" between client and server, this class maintains envelope data
 * (recipients and sender) as "provisional" until they are "confirmed". Confirmed means the server accepted, provisional
 * means the request was issued by client yet final disposition is unknown. <br>
 * <br>
 * This class also implements {@link com.untangle.uvm.util.TemplateValues TemplateValues}. Valid key names which can be
 * derefferenced from an SmtpTransaction begin with the literal <code>$SMTPTransaction:</code> followed by any of the
 * following tokens:
 * <ul>
 * <li>
 * <code><b>TO</b></code> The "TO" addresses, as passed in any "RCPT TO:" commands. Each recipient is on its own line.
 * Even if there is only one recipient, this variable will be substituted with the value of the recipient <i>followed by
 * a CRLF.</i> If there are no recipients (?!?), than this will simply return a blank String ("").</li>
 * <li>
 * <code><b>FROM</b></code> The "FROM" as passed in the "MAIL FROM:" command. If there is no FROM value (or it was
 * "&lt;>"), the literal "&lt;>" will be substituted.</li>
 * </ul>
 */
public final class SmtpTransaction implements TemplateValues
{

    private static final String SMTP_TX_TEMPLATE_PREFIX = "SMTPTransaction:".toLowerCase();
    private static final String TO_TV = "TO".toLowerCase();
    private static final String FROM_TV = "FROM".toLowerCase();
    private final Logger logger = Logger.getLogger(SmtpImpl.class);

    /**
     * Enum of Transaction states.
     */
    public enum TransactionState
    {
        OPEN, COMMITTED, RESET, FAILED
    }

    private TransactionState state = TransactionState.OPEN;
    private InternetAddress from;
    private boolean fromConfirmed;
    private List<EmailAddressWithStatus> recipients;
    private boolean hasAtLeastOneRecipient = false;

    /**
     * Initialize instance of SmtpTransaction.
     * @return Instance of SmtpTransaction.
     */
    public SmtpTransaction()
    {
        this.recipients = new ArrayList<EmailAddressWithStatus>();
        this.from = null;
        this.fromConfirmed = false;
    }

    /**
     * Access the state of the transaction
     * @return Current transaxction state
     */
    public TransactionState getState()
    {
        return this.state;
    }

    /**
     * Change the state to "COMITTED"
     */
    public void commit()
    {
        this.state = TransactionState.COMMITTED;
    }

    /**
     * Change the state to "RESET"
     */
    public void reset()
    {
        this.state = TransactionState.RESET;
    }

    /**
     * Change the state to "FAILED"
     */
    public void failed()
    {
        this.state = TransactionState.FAILED;
    }

    /**
     * Test if this transaction is still open.
     * @return true if open state, false if not.
     */
    public boolean isOpen()
    {
        return this.state == TransactionState.OPEN;
    }

    /**
     * Get the recipients ("RCPT TO...") for this Transaction.
     * 
     * @param confirmedOnly
     *            if true, only those recipients who have been positivly acknowledged by the server are returned.
     * 
     * @return List of InternetAddress recipients.
     */
    public List<InternetAddress> getRecipients(boolean confirmedOnly)
    {
        List<InternetAddress> ret = new ArrayList<InternetAddress>();
        for (EmailAddressWithStatus eaws : this.recipients) {
            if ((confirmedOnly && eaws.confirmed) || (!confirmedOnly)) {
                ret.add(eaws.addr);
            }
        }
        return ret;
    }

    /**
     * A recipient has been requested (an "RCPT TO...") issued. Queue the recipient provisionally.
     * @param addr InternetAddress of email address.
     */
    public void toRequest(InternetAddress addr)
    {
        this.recipients.add(new EmailAddressWithStatus(addr));
    }

    /**
     * The server has responded to a previous RCPT TO... request. The Transaction should change its internal recipient
     * collection accordingly
     *
     * @param addr      InternetAddress of email address.
     * @param accept
     *            if true, the server accepted the recipient.
     */
    public void toResponse(InternetAddress addr, boolean accept)
    {
        // In case someone (dumb) attempts to request the same recipient twice, scan from top-down for the to
        this.hasAtLeastOneRecipient = true;

        for (int i = 0; i < this.recipients.size(); i++) {
            EmailAddressWithStatus eaws = this.recipients.get(i);
            if (!eaws.addr.equals(addr)) {
                continue;
            }
            if (accept) {
                eaws.confirmed = true;
            } else {
                this.recipients.remove(i);
            }
            return;
        }
        // If we're here, there is a programming error with the caller
        logger.warn("Did not find email address "+addr.getAddress());
        EmailAddressWithStatus eaws = new EmailAddressWithStatus(addr);
        eaws.confirmed = true;
        this.recipients.add(eaws);
    }

    /**
     * Quick test to see if there is at least one {@link #toResponse confirmed recipient}.
     * 
     * @return true if at least one recipient has been confirmed.
     */
    public boolean hasAtLeastOneConfirmedRecipient()
    {
        return this.hasAtLeastOneRecipient;
    }

    /**
     * The client has issued a "MAIL FROM..." command. The transaction will record this address as the FROM
     * provisionally.
     * @param addr InternetAddress of email address.
     */
    public void fromRequest(InternetAddress addr)
    {
        this.from = addr;
        this.fromConfirmed = false;
    }

    /**
     * Change the internal envelope data to reflect the server's response to the "MAIL" command
     *
     * @param addr InternetAddress of email address.
     * @param accept
     *            did the server accept the address.
     */
    public void fromResponse(InternetAddress addr, boolean accept)
    {
        if (this.from == null) {
            logger.warn("this.from is null ");
            return;
        }
        if (accept) {
            this.fromConfirmed = true;
        } else {
            this.from = null;
            this.fromConfirmed = false;
        }
    }

    /**
     * Get the FROM for the envelope. May be null. To test if this has been confirmed, use {@link #isFromConfirmed}
     * @return InternetAddress email address of from address.
     */
    public InternetAddress getFrom()
    {
        return this.from;
    }

    /**
     * Test if the FROM has been confirmed by the server.
     * @return true if address confirmed, false if not.
     */
    public boolean isFromConfirmed()
    {
        return this.fromConfirmed;
    }

    /**
     * For use in Templates (see JavaDoc at the top of this class for explanation of vairable format}.
     * @param key String of template key.
     * @return String of template value.
     */
    public String getTemplateValue(String key)
    {
        key = key.trim().toLowerCase();
        if (key.startsWith(SMTP_TX_TEMPLATE_PREFIX)) {
            key = key.substring(SMTP_TX_TEMPLATE_PREFIX.length());
            if (key.equals(TO_TV)) {
                StringBuilder sb = new StringBuilder();
                for (EmailAddressWithStatus eaws : this.recipients) {
                    sb.append(MIMEUtil.toSMTPString(eaws.addr));
                    sb.append(CRLF);
                }
                return sb.toString();
            } else if (key.equals(FROM_TV)) {
                return (this.from == null || this.from.getAddress()==null || this.from.getAddress().trim().length()==0) ? "<>" : MIMEUtil.toSMTPString(this.from);
            }
        }
        return null;
    }

    /**
     * Email address with confirmed status.
     */
    private class EmailAddressWithStatus
    {
        final InternetAddress addr;
        boolean confirmed;

        /**
         * Initialize instance of EmailAddressWithStatus.
         * @param addr InternetAddress of email to set.
         * @return instance of EmailAddressWithStatus with confirmed set to false
         */
        EmailAddressWithStatus(InternetAddress addr) {
            this.addr = addr;
            this.confirmed = false;
        }
    }

}
