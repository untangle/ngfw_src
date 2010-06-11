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
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;


/**
 * This is a test tag
 */
@SuppressWarnings("serial")
public class HelloTag extends
                          TagSupport {

    private String m_message = null;

    public void setMessage(String value){
        m_message = value;
    }

    public String getMessage(){
        return m_message;
    }


    public int doStartTag() {
        try {
            JspWriter out = pageContext.getOut();
            out.println(m_message);
        }
        catch (Exception ex) {
            throw new Error("Something went wrong");
        }
        return SKIP_BODY;
    }
}
