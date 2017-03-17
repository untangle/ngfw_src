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

    public InboxRecord() {
    }

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
     */
    public final String[] getRecipients()
    {
        return recipients;
    }

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

    public final void setMailID(String id)
    {
        this.mailID = id;
    }

    /**
     * Get the date (millis since 1970, GMT) that this file was placed into the quarantine. This is <b>not</b> the DATE
     * on the MIME message.
     */
    public final long getInternDate()
    {
        return addedOn;
    }

    public final void setInternDate(long date)
    {
        this.addedOn = date;
    }

    /**
     * Get the summary of the mail
     */
    public final MailSummary getMailSummary()
    {
        return mailSummary;
    }

    public final void setMailSummary(MailSummary summary)
    {
        this.mailSummary = summary;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(getMailID()).append(", Date: ").append("" + getInternDate()).append(", Summary:[")
                .append(getMailSummary()).append("]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof InboxRecord)) {
            return false;
        }
        return ((InboxRecord) other).getMailID().equals(getMailID());
    }

    @Override
    public int hashCode()
    {
        return getMailID().hashCode();
    }
}
