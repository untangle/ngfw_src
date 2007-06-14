/*
 * $HeadURL:$
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
