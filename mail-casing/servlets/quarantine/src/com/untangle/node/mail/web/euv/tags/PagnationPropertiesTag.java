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

import javax.servlet.ServletRequest;

import com.untangle.node.mail.papi.quarantine.InboxRecordCursor;
import com.untangle.node.mail.web.euv.Util;

public final class PagnationPropertiesTag extends SingleValueTag {

    private static final String KEY = "untangle.pagnationproperties.rowsperpage";
    private String m_propName;

    public String getPropName() {
        return m_propName;
    }
    public void setPropName(String n) {
        m_propName = n;
    }

    public static final void setCurrentRowsPerPAge(ServletRequest request,
                                                   String rows) {
        request.setAttribute(KEY, rows);
    }
    public static final void clearCurretRowsPerPAge(ServletRequest request) {
        request.removeAttribute(KEY);
    }

    /**
     * Returns null if there is no current number of rows
     */
    public static String getCurrentRowsPerPAge(ServletRequest request) {
        return (String) request.getAttribute(KEY);
    }
    static boolean hasCurrentRowsPerPAge(ServletRequest request) {
        return getCurrentRowsPerPAge(request) != null;
    }

    @Override
    protected String getValue() {
        InboxRecordCursor cursor = InboxIndexTag.getCurrentIndex(pageContext.getRequest());
        if(cursor == null) {
            return "";
        }

        if(getPropName().equalsIgnoreCase("sorting")) {
            return Util.sortByToString(cursor.getSortedBy());
        }
        else if(getPropName().equalsIgnoreCase("ascending")) {
            return "" + cursor.isAscending();
        }
        else if(getPropName().equalsIgnoreCase("prevId")) {
            return "" + cursor.getPrevStartingAt();
        }
        else if(getPropName().equalsIgnoreCase("nextId")) {
            return "" + cursor.getNextStartingAt();
        }
        else if(getPropName().equalsIgnoreCase("thisId")) {
            return "" + cursor.getCurrentStartingAt();
        }
        else if(getPropName().equalsIgnoreCase("rPPOption")) {
            return getCurrentRowsPerPAge(pageContext.getRequest());
        }

        return "";
    }
}
