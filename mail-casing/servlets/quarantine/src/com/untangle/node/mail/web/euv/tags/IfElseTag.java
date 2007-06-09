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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;


/**
 * Conditionaly includes page chunk if
 * something is true (or false)
 */
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
            return EVAL_BODY_TAG;
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
