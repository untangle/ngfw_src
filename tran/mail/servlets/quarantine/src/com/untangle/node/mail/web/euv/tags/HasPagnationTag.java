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

import com.untangle.node.mail.papi.quarantine.InboxRecordCursor;

/**
 * Includes/excludes body chunks if there
 * is a "prev" or "next" page to be shown
 */
public final class HasPagnationTag
    extends IfElseTag {

    private String m_linkType;

    public String getLinkType() {
        return m_linkType;
    }
    public void setLinkType(String t) {
        m_linkType = t;
    }

    @Override
    protected boolean isConditionTrue() {
        InboxRecordCursor cursor = InboxIndexTag.getCurrentIndex(pageContext.getRequest());
        if(cursor == null) {
            return false;
        }

        if(getLinkType().equals("prev")) {
            return cursor.hasPrev();
        }
        else {
            return cursor.hasNext();
        }
    }
}
