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
package com.untangle.node.mail.web.euv.tags;

import javax.servlet.jsp.PageContext;


/**
 * Works with RPPIteratorTag
 *
 */
public final class RPPCurrentOptionTag
    extends SingleValueTag {

    private static final String KEY = "untangle.RPPCurrentOptionTag";


    @Override
    protected String getValue() {
        return getCurrent(pageContext);
    }

    /**
     * Returns null if not found
     */
    public static String getCurrent(PageContext pageContext) {
        return (String) pageContext.getAttribute(KEY);
    }

    public static void setCurrent(PageContext pageContext, String s) {
        pageContext.setAttribute(KEY, s, PageContext.PAGE_SCOPE);
    }
}
