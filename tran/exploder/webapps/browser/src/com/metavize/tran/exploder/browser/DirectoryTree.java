/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.exploder.browser;

import java.io.IOException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

public class DirectoryTree extends TagSupport
{
    private String url = null;

    public void setUrl(String value)
    {
        this.url = value;
    }

    public String getUrl()
    {
        return url;
    }

    // TagSupport methods -----------------------------------------------------

    @Override
    public int doStartTag() throws JspException
    {
        try {
            JspWriter out = pageContext.getOut();

            out.println("<span class='trigger' onClick='showDir(\"" + url + "\");'>");
            out.println("<img src='closed.gif' id='I" + url + "'>" + url + "</img>");
            out.println("<br/>");
            out.println("</span>");
            out.println("<span class='dir' id='" + url + "'></span>");

        } catch (IOException exn) {
            throw new JspException("could not emit tree", exn);
        }

        return SKIP_BODY;
    }
}
