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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.MvvmLocalContext;
import org.apache.log4j.Logger;

public class InstallRequestServlet extends HttpServlet
{
    private boolean readOnly;

    private final Logger logger = Logger.getLogger(getClass());

    @Override
    public void init()
    {
        String s = System.getProperty("mvvm.settings.readonly");
        readOnly = Boolean.parseBoolean(s);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        if (readOnly) {
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
