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
