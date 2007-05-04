/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: MaxDaysIdleInboxTag.java 7988 2006-11-09 03:45:12Z amread $
 */
package com.untangle.tran.mail.web.euv.tags;

import javax.servlet.ServletRequest;

/**
 * Outputs the max days an inbox can remain idle or 28 if not set
 */
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
