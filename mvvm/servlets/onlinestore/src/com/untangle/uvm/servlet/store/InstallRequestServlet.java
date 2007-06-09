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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmLocalContext;
import com.untangle.mvvm.security.MvvmPrincipal;
import org.apache.log4j.Logger;

public class InstallRequestServlet extends HttpServlet
{
    private final Logger logger = Logger.getLogger(getClass());

    @Override
    public void init()
    {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        MvvmPrincipal p = (MvvmPrincipal)req.getUserPrincipal();

        if (p.isReadOnly()) {
            logger.debug("ignoring install request in read-only mode");
        } else {
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
}
