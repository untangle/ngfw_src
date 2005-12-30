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

import java.io.IOException;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

public class Hello extends BodyTagSupport
{
    private String name = null;
    private int iterations = 1;

    public void setName(String value)
    {
        name = value;
    }

    public String getName()
    {
        return name;
    }

    public void setIterations(String value)
    {
        try {
            iterations = Integer.parseInt(value);
        } catch (NumberFormatException exn) {
            iterations = 1;
        }
    }

    public String getIterations()
    {
        return Integer.toString(iterations);
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

        return EVAL_BODY_TAG;
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

    public int doAfterBody() throws JspTagException
    {
        if (iterations-- >= 1) {
            BodyContent body = getBodyContent();
            try {
                JspWriter out = body.getEnclosingWriter();
                out.println(body.getString());
                body.clearBody();
            } catch (IOException exn) {
                throw new JspTagException("Error in Hello Tag doAfterBody " + exn);
            }
            return EVAL_BODY_TAG;
        } else {
            return SKIP_BODY;
        }
    }
}
