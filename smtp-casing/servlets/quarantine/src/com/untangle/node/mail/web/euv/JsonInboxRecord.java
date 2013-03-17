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
