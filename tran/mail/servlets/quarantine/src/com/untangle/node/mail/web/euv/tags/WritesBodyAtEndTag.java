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
 * Base class for BodyTags which output their
 * body at "doEndTag"
 */
public abstract class WritesBodyAtEndTag
    extends BodyTagSupport {

    private boolean m_isEmpty = false;

    protected void mvSetEmpty() {
        m_isEmpty = true;
    }

    public int doEndTag() throws JspException{
        try {
            BodyContent body = getBodyContent();
            if(m_isEmpty || body == null || body.getString() == null) {
                m_isEmpty = false;
                return EVAL_PAGE;
            }
            JspWriter writer = body.getEnclosingWriter();
            String bodyString = body.getString();
            writer.println(bodyString);
            return EVAL_PAGE;
        }
        catch (Exception ioe){
            ioe.printStackTrace(System.out);
            throw new JspException(ioe.getMessage());
        }
    }

    public void release() {
        m_isEmpty = false;
        super.release();
    }
}
