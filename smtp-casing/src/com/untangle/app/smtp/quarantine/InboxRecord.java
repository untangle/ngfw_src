/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine;

import java.io.Serializable;

/**
 * Representation of a mail quarantined within a given {@link com.untangle.app.smtp.quarantine.InboxIndex inbox}. <br>
 * <br>
 * Users are not supposed to subclass this. Subclassing is for the internals of the quarantine system (but may be
 * visible to you because of classloader issues).
 */
@SuppressWarnings("serial")
public final class InboxRecord implements Serializable
{

    private String mailID;
    private long addedOn;
    private MailSummary mailSummary;
    private String[] recipients;

    /**
     * Initialize instance of InboxRecord.
     * @return Instance of InboxRecord.
     */
    public InboxRecord() {
    }

    /**
     * Initialize instance of InboxRecord.
     * @param  mailID     Message ID.
     * @param  addedOn    Addedon.
     * @param  summary    MailSummary
     * @param  recipients List of recipients.
     * @return Instance of InboxRecord.
     */
    public InboxRecord(String mailID, long addedOn, MailSummary summary, String[] recipients) {

        this.mailID = mailID;
        this.addedOn = addedOn;
        this.mailSummary = summary;
        this.recipients = recipients;
    }

    /**
     * Get the recipients <b>for this inbox entry</b>. This does not mean all recipients of the email. This refers to
     * the destination(s) of the mail which were re-routed to quarantine. For the cases of address remapping, there may
     * be more than one recipient, and the owner of the inbox may not be among the recipients.
     * @return Array of addresses.
     */
    public final String[] getRecipients()
    {
        return recipients;
    }

    /**
     * Write list of recipients
     * @param recipients Array of recipient addresses.
     */
    public final void setRecipients(String[] recipients)
    {
        this.recipients = recipients;
    }

    /**
     * Get the unique (within the scope of a given inbox) ID for this mail
     * 
     * @return the unique ID
     */
    public final String getMailID()
    {
        return mailID;
    }

    /**
     * Write mail ID.
     * @param id Mail identifier.
     */
    public final void setMailID(String id)
    {
        this.mailID = id;
    }

    /**
     * Get the date (millis since 1970, GMT) that this file was placed into the quarantine. This is <b>not</b> the DATE
     * on the MIME message.
     * @return long of date.
     */
    public final long getInternDate()
    {
        return addedOn;
    }

    /**
     * Set the date in milis.
     * @param date long of timestamp.
     */
    public final void setInternDate(long date)
    {
        this.addedOn = date;
    }

    /**
     * Get the summary of the mail
     * @return MaillSummary.
     */
    public final MailSummary getMailSummary()
    {
        return mailSummary;
    }

    /**
     * Write email summary.
     * @param summary MailSummary.
     */
    public final void setMailSummary(MailSummary summary)
    {
        this.mailSummary = summary;
    }

    /**
     * Create summary of record.
     * @return String of record.
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(getMailID()).append(", Date: ").append("" + getInternDate()).append(", Summary:[")
                .append(getMailSummary()).append("]");
        return sb.toString();
    }

    /**
     * Compare mail ids.
     * @param  other Mail id to compare.
     * @return       If true the same, false otherwise.
     */
    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof InboxRecord)) {
            return false;
        }
        return ((InboxRecord) other).getMailID().equals(getMailID());
    }

    /**
     * Return hashcode of mailid.
     * @return Return hashcode for the current mailid.
     */
    @Override
    public int hashCode()
    {
        return getMailID().hashCode();
    }
}
