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

import javax.servlet.jsp.PageContext;

import com.untangle.node.mail.papi.quarantine.InboxRecord;
import com.untangle.node.util.JSEscape;


/**
 * Works with InboxIndexTag
 *
 */
@SuppressWarnings("serial")
public final class InboxRecordTag extends SingleValueTag {

    private static final String INBOX_RECORD_PS_KEY = "untangle.inbox_record";

    // constant values must be in lower case
    public static final String MAILID_PROP = "mailid";
    public static final String SENDER_PROP = "sender";
    public static final String TSENDER_PROP = "tsender"; // truncated
    public static final String TSUBJECT_PROP = "tsubject"; // truncated
    public static final String FSCORE_PROP = "fdetail"; // formatted
    public static final String FDATE_PROP = "fdate"; // formatted
    public static final String FTIME_PROP = "ftime"; // formatted
    public static final String FSIZE_PROP = "fsize"; // formatted

    private String m_propName;
    private boolean m_jsEscape = true;

    public void setProp(String s) {
        m_propName = s;
    }
    public String getProp() {
        return m_propName;
    }

    public void setJSEscape(boolean escape) {
        m_jsEscape = escape;
    }

    public boolean isJSEscape() {
        return m_jsEscape;
    }

    @Override
    protected String getValue() {
        String ret = propNameToProp(getCurrent(pageContext), getProp());
        ret = m_jsEscape?JSEscape.escapeJS(ret):ret;
        return ret;
    }

    private String propNameToProp(InboxRecord record, String name) {
        if(record == null || name == null) {
            return null;
        }
        name = name.trim().toLowerCase();
        if(name.equals(MAILID_PROP)) {
            return record.getMailID();
        }
        if(name.equals(SENDER_PROP)) {
            return record.getMailSummary().getSender();
        }
        if(name.equals(TSENDER_PROP)) {
            return record.getMailSummary().getTruncatedSender();
        }
        if(name.equals(TSUBJECT_PROP)) {
            return record.getMailSummary().getTruncatedSubject();
        }
        if(name.equals(FSCORE_PROP)) {
            return record.getMailSummary().getFormattedQuarantineDetail();
        }
        if(name.equals(FDATE_PROP)) {
            return record.getFormattedDate();
        }
        if(name.equals(FTIME_PROP)) {
            return record.getFormattedTime();
        }
        if(name.equals(FSIZE_PROP)) {
            return record.getFormattedSize();
        }
        return null;
    }

    /**
     * Returns null if not found
     */
    public static InboxRecord getCurrent(PageContext pageContext) {
        return (InboxRecord) pageContext.getAttribute(INBOX_RECORD_PS_KEY);
    }

    public static void setCurrent(PageContext pageContext, InboxRecord record) {
        pageContext.setAttribute(INBOX_RECORD_PS_KEY, record, PageContext.PAGE_SCOPE);
    }

    public void release() {
        m_jsEscape = true;
        super.release();
    }
}
