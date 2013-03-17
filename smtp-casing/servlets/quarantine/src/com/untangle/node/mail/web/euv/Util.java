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

import java.util.HashMap;
import javax.servlet.ServletRequest;

import com.untangle.node.smtp.quarantine.InboxRecordComparator;

/**
 * Utility methods
 */
public class Util {
    private static HashMap<String, InboxRecordComparator.SortBy> m_stringToSB = new HashMap<String, InboxRecordComparator.SortBy>();
    private static HashMap<InboxRecordComparator.SortBy, String> m_sbToString = new HashMap<InboxRecordComparator.SortBy, String>();

    static {
        InboxRecordComparator.SortBy[] allSortings = InboxRecordComparator.SortBy.values();
        for(int i = 0; i < allSortings.length; i++) {
            String key = Integer.toString(i);
            m_stringToSB.put(key, allSortings[i]);
            m_sbToString.put(allSortings[i], key);
        }
    }

    public static String sortByToString(InboxRecordComparator.SortBy sb) {
        return m_sbToString.get(sb);
    }

    /**
     * I didn't want the actual Strings from the
     * enum to travel to the client, but for now I'll
     * use this little hack.  This goes from String to
     * SortBy.  {@link #sortByToString sortByToString} does
     * the reverse.
     * <br><br>
     * @param s the String representation of sorting
     * @param def the default, should <code>s</code> not be
     *        convertable to a SortBy
     */
    public static InboxRecordComparator.SortBy stringToSortBy(String s, InboxRecordComparator.SortBy def) {
        if(s == null) {
            return def;
        }
        s = s.trim().toLowerCase();

        InboxRecordComparator.SortBy ret = m_stringToSB.get(s);

        return ret == null ? def : ret;
    }

    /**
     * Read a boolean parameter
     */
    public static boolean readBooleanParam(ServletRequest req, String paramName, boolean def) {
        String parameter = req.getParameter(paramName);
        if(parameter == null) {
            return def;
        }
        try {
            return Boolean.parseBoolean(parameter);
        }
        catch(Exception ex) { }

        return def;
    }

    /**
     * Read an int parameter
     */
    public static int readIntParam(ServletRequest req, String paramName, int def) {
        String parameter = req.getParameter(paramName);
        if(parameter == null) {
            return def;
        }
        try {
            return Integer.parseInt(parameter);
        }
        catch(Exception ex) { }

        return def;
    }
}
