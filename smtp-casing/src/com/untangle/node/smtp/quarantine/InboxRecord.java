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

package com.untangle.node.smtp.quarantine;

import java.io.Serializable;

/**
 * Representation of a mail quarantined within a given
 * {@link com.untangle.node.smtp.quarantine.InboxIndex inbox}. <br>
 * <br>
 * Users are not supposed to subclass this. Subclassing is for the internals of
 * the quarantine system (but may be visible to you because of classloader
 * issues).
 */
@SuppressWarnings("serial")
public final class InboxRecord implements Serializable
{

    private String m_mailID;
    private long m_addedOn;
    private MailSummary m_mailSummary;
    private String[] m_recipients;

    public InboxRecord() {
    }

    public InboxRecord(String mailID, long addedOn, MailSummary summary, String[] recipients) {

        m_mailID = mailID;
        m_addedOn = addedOn;
        m_mailSummary = summary;
        m_recipients = recipients;
    }

    /**
     * Get the recipients <b>for this inbox entry</b>. This does not mean all
     * recipients of the email. This refers to the destination(s) of the mail
     * which were re-routed to quarantine. For the cases of address remapping,
     * there may be more than one recipient, and the owner of the inbox may not
     * be among the recipients.
     */
    public final String[] getRecipients()
    {
        return m_recipients;
    }

    public final void setRecipients(String[] recipients)
    {
        m_recipients = recipients;
    }

    /**
     * Get the unique (within the scope of a given inbox) ID for this mail
     * 
     * @return the unique ID
     */
    public final String getMailID()
    {
        return m_mailID;
    }

    public final void setMailID(String id)
    {
        m_mailID = id;
    }

    /**
     * Get the date (millis since 1970, GMT) that this file was placed into the
     * quarantine. This is <b>not</b> the DATE on the MIME message.
     */
    public final long getInternDate()
    {
        return m_addedOn;
    }

    public final void setInternDate(long date)
    {
        m_addedOn = date;
    }

    /**
     * Get the summary of the mail
     */
    public final MailSummary getMailSummary()
    {
        return m_mailSummary;
    }

    public final void setMailSummary(MailSummary summary)
    {
        m_mailSummary = summary;
    }

    /**
     * Get the size of the mail's MIME file.
     */
    public final long getSize()
    {
        return m_mailSummary.getQuarantineSize();
    }

    public final void setSize(long size)
    {
        m_mailSummary.setQuarantineSize(size);
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
