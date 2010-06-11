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
package com.untangle.node.mail.web.euv.tags;

import com.untangle.node.mail.papi.quarantine.InboxRecord;


/**
 * Works with InboxIndexTag (i.e. must be within one).
 *
 */
@SuppressWarnings("serial")
public final class InboxRecordHasAttachmentTag extends IfElseTag {

    @Override
    protected boolean isConditionTrue()
    {
        InboxRecord record = InboxRecordTag.getCurrent(pageContext);
        return record==null?false:record.getMailSummary().getAttachmentCount() > 0;
    }
}


