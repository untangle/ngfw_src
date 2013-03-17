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
package com.untangle.node.smtp.web.euv.tags;

import javax.servlet.ServletRequest;

/**
 * Outputs the max days an inbox can remain idle or 28 if not set
 */
@SuppressWarnings("serial")
public final class MaxDaysIdleInboxTag extends SingleValueTag {
    private static final String DAYS_IDLE_INBOX = "untangle.days.idle_inbox";

    @Override
    protected String getValue() {
        return getMaxDays(pageContext.getRequest());
    }

    public static final void setMaxDays(ServletRequest request, String maxDays) {
        request.setAttribute(DAYS_IDLE_INBOX, maxDays);
    }

    public static final void clearMaxDays(ServletRequest request) {
        request.removeAttribute(DAYS_IDLE_INBOX);
    }

    /**
     * Returns default of 28 if there is no max days
     */
    static String getMaxDays(ServletRequest request) {
        String maxDays = (String) request.getAttribute(DAYS_IDLE_INBOX);
        if (null == maxDays) {
            return "28";
        }
        return maxDays;
    }
}
