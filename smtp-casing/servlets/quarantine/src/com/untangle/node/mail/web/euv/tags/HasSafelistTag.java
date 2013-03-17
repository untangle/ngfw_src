/**
 * $Id$
 */
package com.untangle.node.smtp.web.euv.tags;

import javax.servlet.ServletRequest;


/**
 * Includes/excludes body chunks if there
 * is (or could be) a Safelist for the current user
 */
@SuppressWarnings("serial")
public final class HasSafelistTag extends IfElseTag
{
    private static final String HAS_ENTRY_KEY = "untangle.safelist.exists";

    @Override
    protected boolean isConditionTrue() {
        Boolean ret = (Boolean) pageContext.getRequest().getAttribute(HAS_ENTRY_KEY);
        return ret==null ? false: ret.booleanValue();
    }

    public static void setCurrent(ServletRequest req, boolean hasEntry) {
        req.setAttribute(HAS_ENTRY_KEY, hasEntry?Boolean.TRUE:Boolean.FALSE);
    }
}
