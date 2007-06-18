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
