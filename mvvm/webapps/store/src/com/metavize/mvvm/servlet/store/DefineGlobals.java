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

package com.metavize.mvvm.servlet.store;

import java.io.IOException;
import javax.security.auth.login.FailedLoginException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.toolbox.MackageDesc;
import com.metavize.mvvm.toolbox.ToolboxManager;

public class DefineGlobals extends TagSupport
{
    @Override
    public int doStartTag() throws JspException
    {
        try {
            MvvmLocalContext ctx = MvvmContextFactory.context();

            JspWriter out = pageContext.getOut();

            out.println("var installed = new Array();");

            ToolboxManager tool = ctx.toolboxManager();
            for (MackageDesc md : tool.installed()) {
                out.println("installed['" + md.getName() + "'] = '"
                            + md.getInstalledVersion() + "';");
            }
        } catch (IOException exn) {
            throw new JspException("could not emit packages", exn);
        }

        return SKIP_BODY;
    }
}
