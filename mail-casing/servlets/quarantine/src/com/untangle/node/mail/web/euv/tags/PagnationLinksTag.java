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

import java.net.URLEncoder;

import com.untangle.node.mail.papi.quarantine.InboxRecordCursor;
import com.untangle.node.mail.web.euv.Constants;
import com.untangle.node.mail.web.euv.Util;

/**
 * Constructs the "next/prev" query string.  Does <b>not</b>
 * check if there <i>should</i> be "prev/next" links.
 * <br><br>
 * Values for LinkType property are either "prev" or "next"
 *
 */
public final class PagnationLinksTag
    extends SingleValueTag {

    private String m_linkType;

    public String getLinkType() {
        return m_linkType;
    }
    public void setLinkType(String t) {
        m_linkType = t;
    }

    //InboxRecordCursor getCurrentIndex(ServletRequest request) {

    @Override
    protected String getValue() {
        InboxRecordCursor cursor = InboxIndexTag.getCurrentIndex(pageContext.getRequest());
        if(cursor == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(Constants.INBOX_MAINTENENCE_CTL).append('?');
        addKVP(Constants.ACTION_RP, Constants.VIEW_INBOX_RV, sb);
        sb.append('&');
        addKVP(Constants.AUTH_TOKEN_RP, CurrentAuthTokenTag.getCurrent(pageContext.getRequest()), sb);
        sb.append('&');
        addKVP(Constants.SORT_BY_RP, Util.sortByToString(cursor.getSortedBy()), sb);
        sb.append('&');
        addKVP(Constants.SORT_ASCEND_RP, "" + cursor.isAscending(), sb);
        sb.append('&');
        if(getLinkType().equalsIgnoreCase("prev")) {
            addKVP(Constants.FIRST_RECORD_RP, "" + cursor.getPrevStartingAt(), sb);
        }
        else {
            addKVP(Constants.FIRST_RECORD_RP, "" + cursor.getNextStartingAt(), sb);
        }
        sb.append('&');
        addKVP(Constants.ROWS_PER_PAGE_RP, "" + cursor.getCurrentRowsPerPage(), sb);

        return sb.toString();
    }

    private static void addKVP(String key, String value, StringBuilder sb) {
        sb.append(key).append('=').append(/*URLEncoder.encode(*/value/*)*/);
    }
}
