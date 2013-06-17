/**
 * $Id$
 */
package com.untangle.node.smtp.web.euv.tags;

import javax.servlet.ServletRequest;


/**
 * Includes/excludes body chunks if the
 * current inbox receives remappings from other inboxes
 */
@SuppressWarnings("serial")
public final class IsReceivesRemapsTag extends IfElseTag {

    private static final String RECEIVES_REMAPS_KEY = "untangle.remapping.IsReceivesRemapsTag";

    @Override
    protected boolean isConditionTrue() {
        Boolean ret = (Boolean) pageContext.getRequest().getAttribute(RECEIVES_REMAPS_KEY);
        return ret==null?
            false:
        ret.booleanValue();
    }

    public static void setCurrent(ServletRequest req, boolean receivesRemaps) {
        req.setAttribute(RECEIVES_REMAPS_KEY,
                         receivesRemaps?Boolean.TRUE:Boolean.FALSE);
    }
}
