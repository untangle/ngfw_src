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
