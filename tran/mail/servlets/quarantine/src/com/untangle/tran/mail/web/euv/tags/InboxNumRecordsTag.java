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
package com.untangle.tran.mail.web.euv.tags;

import com.untangle.tran.mail.papi.quarantine.InboxRecordCursor;

/**
 * Outputs the total number of records in the current index, or
 * unknown if there is no current index
 *
 */
public final class InboxNumRecordsTag extends SingleValueTag {
    @Override
    protected String getValue() {
        InboxRecordCursor iCursor = InboxIndexTag.getCurrentIndex(pageContext.getRequest());
        try {
            return Long.toString(iCursor == null ? 0 : iCursor.inboxCount()) + " mails";
        }
        catch(Exception ex) { return "<unknown> mails"; }
    }
}
