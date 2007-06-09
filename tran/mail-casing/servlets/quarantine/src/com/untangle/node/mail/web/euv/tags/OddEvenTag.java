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
 * Tag which outputs a given String if the "row"
 * is Odd or even.  Works with iterators.
 *
 */
public final class OddEvenTag
    extends SingleValueTag {

    public static final String IS_EVEN_KEY = "untangle.isEven";

    private String m_evenStr;
    private String m_oddStr;

    public void setEven(String str) {
        m_evenStr = str;
    }
    public String getEven() {
        return m_evenStr;
    }

    public void setOdd(String str) {
        m_oddStr = str;
    }
    public String getOdd() {
        return m_oddStr;
    }

    @Override
    protected String getValue() {
        Boolean isEven = (Boolean) pageContext.getAttribute(IS_EVEN_KEY, PageContext.PAGE_SCOPE);

        return isEven==null?
            null:isEven.booleanValue()?getEven():getOdd();
    }

    public static void setCurrent(PageContext pageContext, boolean isEven) {
        pageContext.setAttribute(IS_EVEN_KEY,
                                 isEven?Boolean.TRUE:Boolean.FALSE,//Will this autobox?
                                 PageContext.PAGE_SCOPE);
    }
}
