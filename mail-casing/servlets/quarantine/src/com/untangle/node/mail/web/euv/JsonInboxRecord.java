/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.mail.web.euv;

import java.util.Date;

import com.untangle.node.mail.papi.quarantine.InboxRecord;
import com.untangle.node.mail.papi.quarantine.MailSummary;

public class JsonInboxRecord
{
    private String[] recipients;
    private String mailID;
    private Date quarantinedDate;
    private int size;
    private int attachmentCount;
    private String truncatedSender;
    private String sender;
    private String truncatedSubject;
    private String subject;
    private String quarantineCategory;
    private String quarantineDetail;
    private int quarantineSize;

    JsonInboxRecord( InboxRecord record )
    {
        MailSummary ms = record.getMailSummary();
        this.recipients = record.getRecipients();
        this.mailID = record.getMailID();
        this.quarantinedDate = new Date( record.getInternDate());
        this.size = (int)record.getSize();
        this.attachmentCount = ms.getAttachmentCount();
        this.truncatedSender = ms.getTruncatedSender();
        this.sender = ms.getSender();
        this.truncatedSubject = ms.getTruncatedSubject();
        this.subject = ms.getSubject();
        this.quarantineCategory = ms.getQuarantineCategory();
        this.quarantineDetail = ms.getQuarantineDetail();
        this.quarantineSize = (int)ms.getQuarantineSize();
    }

    public String[] getRecipients()
    {
        return this.recipients;
    }

    public void setRecipients( String[] newValue )
    {
        this.recipients = newValue;
    }
        
    public String getMailID()
    {
        return this.mailID;
    }

    public void setMailID( String newValue )
    {
        this.mailID = newValue;
    }

    public Date getQuarantinedDate()
    {
        return this.quarantinedDate;
    }

    public void setQuarantinedDate( Date newValue )
    {
        this.quarantinedDate = newValue;
    }

    public int getSize()
    {
        return this.size;
    }

    public void setSize( int newValue )
    {
        this.size = newValue;
    }

    /* Everything past here is from MailSummary */
    public int getAttachmentCount()
    {
        return this.attachmentCount;
    }

    public void setAttachmentCount( int newValue )
    {
        this.attachmentCount = newValue;
    }
        
    public String getTruncatedSender()
    {
        return this.truncatedSender;
    }

    public void setTruncatedSender( String newValue )
    {
        this.truncatedSender = newValue;
    }

    public String getSender()
    {
        return this.sender;
    }

    public void setSender( String newValue )
    {
        this.sender = newValue;
    }
        
    public String getTruncatedSubject()
    {
        return this.truncatedSubject;
    }

    public void setTruncatedSubject( String newValue )
    {
        this.truncatedSubject = newValue;
    }
        
    public String getSubject()
    {
        return this.subject;
    }

    public void setSubject( String newValue )
    {
        this.subject = newValue;
    }
        
    public String getQuarantineCategory()
    {
        return this.quarantineCategory;
    }

    public void setQuarantineCategory( String newValue )
    {
        this.quarantineCategory = newValue;
    }

    public String getQuarantineDetail()
    {
        return this.quarantineDetail;
    }

    public void setQuarantineDetail( String newValue )
    {
        this.quarantineDetail = newValue;
    }

    public int getQuarantineSize()
    {
        return this.quarantineSize;
    }

    public void setQuarantineSize( int newValue )
    {
        this.quarantineSize = newValue;
    }
}
