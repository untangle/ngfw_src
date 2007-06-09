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

package com.untangle.tran.mail.papi.quarantine;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Representation of a mail quarantined within
 * a given {@link com.untangle.tran.mail.papi.quarantine.InboxIndex inbox}.
 * <br><br>
 * Users are not supposed to subclass this.  Subclassing
 * is for the internals of the quarantine system (but may
 * be visible to you because of classloader issues).
 */
public abstract class InboxRecord
    implements Serializable {

    private String m_mailID;
    private long m_addedOn;
    private MailSummary m_mailSummary;
    private String[] m_recipients;

    public InboxRecord() {}

    public InboxRecord(String mailID, long addedOn, MailSummary summary,
                       String[] recipients) {

        m_mailID = mailID;
        m_addedOn = addedOn;
        m_mailSummary = summary;
        m_recipients = recipients;
    }

    /**
     * Get the recipients <b>for this inbox entry</b>.  This does
     * not mean all recipients of the email.  This refers to the
     * destination(s) of the mail which were re-routed to quarantine.
     * For the cases of address remapping, there may be more than
     * one recipient, and the owner of the inbox may not be among
     * the recipients.
     */
    public final String[] getRecipients() {
        return m_recipients;
    }
    public final void setRecipients(String[] recipients) {
        m_recipients = recipients;
    }

    /**
     * Get the unique (within the scope of a given inbox)
     * ID for this mail
     *
     * @return the unique ID
     */
    public final String getMailID() {
        return m_mailID;
    }
    public final void setMailID(String id) {
        m_mailID = id;
    }

    /**
     * Get the date (millis since 1970, GMT) that this
     * file was placed into the quarantine.  This
     * is <b>not</b> the DATE on the MIME message.
     */
    public final long getInternDate() {
        return m_addedOn;
    }
    public final void setInternDate(long date) {
        m_addedOn = date;
    }

    /**
     * Get the summary of the mail
     */
    public final MailSummary getMailSummary() {
        return m_mailSummary;
    }
    public final void setMailSummary(MailSummary summary) {
        m_mailSummary = summary;
    }

    /**
     * Get the size of the mail's MIME file.
     */
    public final long getSize() {
        return m_mailSummary.getQuarantineSize();
    }
    public final void setSize(long size) {
        m_mailSummary.setQuarantineSize(size);
    }

    // need get and set pair prefixes for velocity
    public final Date getInternDateAsDate() {
        return new Date(getInternDate());
    }

    public final void setInternDateAsDate(Date date) {
        setInternDate(date.getTime());
    }

    public final String getFormattedDate() {
        try {
            Date iDate = new Date(getInternDate());
            SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yy");
            return dateFormat.format(iDate).toString();
        } catch(Exception ex) { return "<unknown>"; }
    }

    public final void setFormattedDate(String date) {
        String dummy = date;
    }

    public final String getFormattedTime() {
        try {
            Date iDate = new Date(getInternDate());
            SimpleDateFormat dateFormat = new SimpleDateFormat("H:mm");
            return dateFormat.format(iDate).toString();
        } catch(Exception ex) { return "<unknown>"; }
    }

    public final void setFormattedTime(String time) {
        String dummy = time;
    }

    // need get and set pair prefixes for velocity
    public final String getFormattedSize() {
        try {
            // in kilobytes
            return String.format("%01.1f", new Float(getSize() / 1024.0));
        } catch(Exception ex) { return "<unknown>"; }
    }

    public final void setFormattedSize(String size) {
        String dummy = size;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(getMailID()).
            append(", Date: ").append("" + getInternDate()).
            append(", Summary:[").append(getMailSummary()).append("]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof InboxRecord)) {
            return false;
        }
        return ((InboxRecord) other).getMailID().equals(getMailID());
    }

    @Override
    public int hashCode() {
        return getMailID().hashCode();
    }
}
