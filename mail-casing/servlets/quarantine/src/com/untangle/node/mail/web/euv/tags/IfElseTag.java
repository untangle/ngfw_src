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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;


/**
 * Conditionaly includes page chunk if
 * something is true (or false)
 */
@SuppressWarnings("serial")
public abstract class IfElseTag
    extends BodyTagSupport {

    private boolean m_includeIfTrue;

    public void setIncludeIfTrue(boolean i) {
        m_includeIfTrue = i;
    }
    public boolean isIncludeIfTrue() {
        return m_includeIfTrue;
    }

    protected abstract boolean isConditionTrue();

    public final int doStartTag() throws JspException {

        if(isConditionTrue() == isIncludeIfTrue()) {
            return EVAL_BODY_BUFFERED;
        }
        return SKIP_BODY;
    }


    public final int doAfterBody() throws JspException {
        try {
            BodyContent body = getBodyContent();
            JspWriter writer = body.getEnclosingWriter();
            String bodyString = body.getString();
            writer.println(bodyString);
        }
        catch(Exception ex) {
            ex.printStackTrace(System.out);
            throw new JspException(ex.toString());
        }

        return SKIP_BODY;
    }
}
