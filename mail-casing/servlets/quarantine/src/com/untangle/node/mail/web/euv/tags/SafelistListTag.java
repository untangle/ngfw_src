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

import java.util.Arrays;
import java.util.Iterator;
import javax.servlet.ServletRequest;


/**
 * Tag which is used to iterate over the contents of a Safelist
 * <br><br>
 * It then sets-up an OddEvenTag and a MessageTag for each iteration.
 */
public final class SafelistListTag
    extends IteratingTag<String> {

    private static final String SL_KEY = "untangle.safelist.contents.";

    @Override
    protected Iterator<String> createIterator() {
        String[] list = getCurrentList(pageContext.getRequest());

        //    System.out.println("***DEBUG*** Creating iterator, array: " +
        //      (list==null?"null":Integer.toString(list.length)) + "");

        if(list == null || list.length == 0) {
            return null;
        }
        return Arrays.asList(list).iterator();
    }
    @Override
    protected void setCurrent(String s) {
        SafelistEntryTag.setCurrent(pageContext, s);
    }

    public static final void setCurrentList(ServletRequest request,
                                            String[] list) {
        request.setAttribute(SL_KEY, list);
    }
    public static final void clearCurrentList(ServletRequest request) {
        request.removeAttribute(SL_KEY);
    }

    /**
     * Returns null if there are no safelist entries
     * - sort string entries, within list, according to natural, ascending order
     */
    static String[] getCurrentList(ServletRequest request) {
        Object allSLEntries = request.getAttribute(SL_KEY);
        if (null != allSLEntries) {
            Arrays.sort((String[]) allSLEntries);
        }
        return (String[]) allSLEntries;
    }

    static boolean hasCurrentList(ServletRequest request) {
        String[] list = getCurrentList(request);
        return list != null && list.length > 0;
    }
}
