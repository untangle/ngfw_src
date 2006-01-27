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

import com.metavize.mvvm.MackageDesc;
import com.metavize.mvvm.ToolboxManager;
import com.metavize.mvvm.client.MvvmConnectException;
import com.metavize.mvvm.client.MvvmRemoteContext;
import com.metavize.mvvm.client.MvvmRemoteContextFactory;

public class DefineGlobals extends TagSupport
{
    @Override
    public int doStartTag() throws JspException
    {
        MvvmRemoteContextFactory factory = null;
        try {
            factory = MvvmRemoteContextFactory.factory();
            MvvmRemoteContext ctx = factory.systemLogin(0);

            JspWriter out = pageContext.getOut();

            out.println("var installed = Array();");

            ToolboxManager tool = ctx.toolboxManager();
            for (MackageDesc md : tool.installed()) {
                out.println("installed['" + md.getName() + "'] = '"
                            + md.getInstalledVersion() + "';");
            }
        } catch (IOException exn) {
            throw new JspException("could not emit packages", exn);
        } catch (MvvmConnectException exn) {
            throw new JspException("could not log into mvvm", exn);
        } catch (FailedLoginException exn) {
            throw new JspException("could not log into mvvm", exn);
        } finally {
            if (null != factory) {
                factory.logout();
            }
        }

        return SKIP_BODY;
    }
}
