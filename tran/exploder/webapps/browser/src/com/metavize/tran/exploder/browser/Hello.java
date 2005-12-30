/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.exploder.browser;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

public class Hello extends TagSupport
{
    private String name = null;

    public void setName(String value)
    {
        name = value;
    }

    public String getName()
    {
        return name;
    }

    public int doStartTag()
    {
        try {
            JspWriter out = pageContext.getOut();
            out.println("<table border=\"\1\">");
            if (name != null)
                out.println("<tr><td> Hello " + name + " </td></tr>");
            else
                out.println("<tr><td> Hello World </td></tr>");
        } catch (Exception ex) {
            throw new Error("All is not well in the world.");
        }

        return SKIP_BODY;
    }

    public int doEndTag()
    {
        try {
            JspWriter out = pageContext.getOut();
            out.println("</table>");
        } catch (Exception ex){
            throw new Error("All is not well in the world.");
        }

        return EVAL_PAGE;
    }
}
