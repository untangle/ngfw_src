/**
 * $Id$
 */
package com.untangle.node.smtp.web.euv.tags;

import javax.servlet.ServletRequest;

/**
 * Includes/excludes body chunks if the
 * current inbox has been remapped to another
 */
@SuppressWarnings("serial")
public final class IsRemappedTag extends IfElseTag
{
    private static final String IS_REMAPPED_KEY = "untangle.remapping.isRemapped";

    @Override
    protected boolean isConditionTrue() {
        Boolean ret = (Boolean) pageContext.getRequest().getAttribute(IS_REMAPPED_KEY);
        return ret==null?
            false:
        ret.booleanValue();
    }

    public static void setCurrent(ServletRequest req, boolean isRemapped) {
        req.setAttribute(IS_REMAPPED_KEY,
                         isRemapped?Boolean.TRUE:Boolean.FALSE);
    }
}
