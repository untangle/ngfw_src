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

package com.untangle.mvvm.servlet.store;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmLocalContext;

import com.untangle.mvvm.toolbox.MackageDesc;
import com.untangle.mvvm.toolbox.ToolboxManager;

import org.apache.log4j.Logger;

public class InstalledAppliances extends HttpServlet
{
    private final Logger logger = Logger.getLogger(getClass());

    @Override
    public void init()
    {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        MvvmLocalContext ctx = MvvmContextFactory.context();
        ServletOutputStream out = resp.getOutputStream();
        out.println("var installed = new Array();");
        
        ToolboxManager tool = ctx.toolboxManager();
        for (MackageDesc md : tool.installed()) {
            out.println("installed['" + md.getName() + "'] = '" + md.getInstalledVersion() + "';");
        }
    }
}
