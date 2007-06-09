/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.tran.mail.web.euv.tags;

import javax.servlet.ServletRequest;


/**
 * Includes/excludes body chunks if the
 * current inbox has been remapped to another
 */
public final class IsRemappedTag
    extends IfElseTag {

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
