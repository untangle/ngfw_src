/**
 * $Id$
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
