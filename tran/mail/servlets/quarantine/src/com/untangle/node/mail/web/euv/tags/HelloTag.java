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
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;


/**
 * This is a test tag
 */
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
