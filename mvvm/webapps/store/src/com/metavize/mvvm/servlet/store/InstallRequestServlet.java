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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.MvvmContextFactory;

public class InstallRequestServlet extends HttpServlet
{
    private final Logger logger = Logger.getLogger(getClass());

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        String mackageName = req.getParameter("mackage");

        if (null != mackageName) {
            MvvmLocalContext ctx = MvvmContextFactory.context();
            ctx.toolboxManager().requestInstall(mackageName);
        } else {
            try {
                resp.sendError(406, "need mackage-name");
            } catch (IOException exn) {
                logger.warn("could not send error page", exn);
            }
        }
    }
}
