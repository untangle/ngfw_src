/**
 * $Id$
 */
package com.untangle.node.smtp.web.euv;

import java.util.Date;

import com.untangle.node.smtp.quarantine.InboxRecord;

public class JsonInboxRecord
{
    private final InboxRecord record;

    JsonInboxRecord( InboxRecord record )
    {
        this.record = record;
    }

    public String[] getRecipients()
    {
        return this.record.getRecipients();

    }
        
    public String getMailID()
    {
        return this.record.getMailID();
    }

    public Date getQuarantinedDate()
    {
        return new Date( this.record.getInternDate());
    }

    public int getSize()
    {
        return (int)this.record.getSize();
    }

    /* Everything past here is from MailSummary */
    public int getAttachmentCount()
    {
        return this.record.getMailSummary().getAttachmentCount();
    }

    public String getTruncatedSender()
    {
        return this.record.getMailSummary().getTruncatedSender();
    }

    public String getSender()
    {
        return this.record.getMailSummary().getSender();
    }
        
    public String getTruncatedSubject()
    {
        return this.record.getMailSummary().getTruncatedSubject();
    }
        
    public String getSubject()
    {
        return this.record.getMailSummary().getSubject();
    }
        
    public String getQuarantineCategory()
    {
        return this.record.getMailSummary().getQuarantineCategory();
    }

    public String getQuarantineDetail()
    {
        return this.record.getMailSummary().getQuarantineDetail();
    }

    public int getQuarantineSize()
    {
        return (int)this.record.getMailSummary().getQuarantineSize();
    }
}
