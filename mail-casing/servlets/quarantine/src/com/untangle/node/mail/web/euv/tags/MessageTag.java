/*
 * $HeadURL:$
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
 * Really dumb tag which just outputs the
 * contents of the page-scope variable {@link #MESSAGE_PS_KEY MESSAGE_PS_KEY}.
 * <br><br>
 * Works with MessagesSetTag
 *
 */
public final class MessageTag
    extends SingleValueTag {

    private static final String MESSAGE_PS_KEY = "untangle.message";

    @Override
    protected String getValue() {
        return (String) pageContext.getAttribute(MESSAGE_PS_KEY, PageContext.PAGE_SCOPE);
    }

    public static void setCurrent(PageContext pageContext, String msg) {
        pageContext.setAttribute(MESSAGE_PS_KEY, msg, PageContext.PAGE_SCOPE);
    }
}
