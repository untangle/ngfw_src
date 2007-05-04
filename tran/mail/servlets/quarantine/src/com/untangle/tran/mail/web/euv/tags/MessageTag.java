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
