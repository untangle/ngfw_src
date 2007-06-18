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

import java.util.Iterator;
import javax.servlet.ServletRequest;

import com.untangle.node.mail.papi.quarantine.InboxRecord;
import com.untangle.node.mail.papi.quarantine.InboxRecordCursor;


/**
 *
 */
public final class InboxIndexTag
    extends IteratingTag<InboxRecord> {

    private static final String INBOX_CURSOR_KEY = "untangle.inbox_cursor";

    @Override
    protected Iterator<InboxRecord> createIterator() {
        return hasCurrentIndex(pageContext.getRequest())?
            getCurrentIndex(pageContext.getRequest()).iterator():
            null;
    }

    @Override
    protected void setCurrent(InboxRecord s) {
        InboxRecordTag.setCurrent(pageContext, s);
    }

    public static final void setCurrentIndex(ServletRequest request,
                                             InboxRecordCursor index) {
        request.setAttribute(INBOX_CURSOR_KEY, index);
    }
    public static final void clearCurrentIndex(ServletRequest request) {
        request.removeAttribute(INBOX_CURSOR_KEY);
    }

    /**
     * Returns null if there is no index
     */
    static InboxRecordCursor getCurrentIndex(ServletRequest request) {
        return (InboxRecordCursor) request.getAttribute(INBOX_CURSOR_KEY);
    }

    static boolean hasCurrentIndex(ServletRequest request) {
        InboxRecordCursor index = getCurrentIndex(request);
        return index != null && index.size() > 0;
    }
}
