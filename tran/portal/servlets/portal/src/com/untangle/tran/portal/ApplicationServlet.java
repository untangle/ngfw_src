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

package com.untangle.tran.portal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmLocalContext;
import com.untangle.mvvm.portal.Application;
import com.untangle.mvvm.portal.LocalApplicationManager;
import com.untangle.mvvm.portal.LocalPortalManager;
import com.untangle.mvvm.portal.PortalLogin;
import com.untangle.mvvm.portal.PortalUser;
import com.untangle.mvvm.util.XmlUtil;
import org.apache.log4j.Logger;

public class ApplicationServlet extends HttpServlet
{
    private Logger logger;
    private LocalPortalManager portalManager;
    private LocalApplicationManager appManager;

    // HttpServlet methods ----------------------------------------------------

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        HttpSession s = req.getSession();

        resp.setContentType("text/xml");
        resp.addHeader("Cache-Control", "no-cache");

        PortalLogin pl = (PortalLogin)req.getUserPrincipal();

        if (null == pl) {
            logger.warn("no principal: " + this);
        }

        PortalUser pu = portalManager.getUser(pl.getUser());

        if (null == pu) {
            logger.warn("no portal user for login: " + pl.getUser());
            try {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch (IOException exn) {
                logger.warn("could not send error", exn);
            }
            return;
        }

        String command = req.getParameter("command");

        try {
            if (command.equals("ls")) {
                List<Application> apps = appManager.getApplications();
                emitApplications(resp.getWriter(), apps);
            } else {
                logger.warn("bad command: " + command);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (IOException exn) {
            logger.warn("could not write bookmarks", exn);
        }
    }

    @Override
    public void init() throws ServletException
    {
        logger = Logger.getLogger(getClass());
        MvvmLocalContext mctx = MvvmContextFactory.context();
        portalManager = mctx.portalManager();
        appManager = portalManager.applicationManager();
    }

    // private methods --------------------------------------------------------

    private void emitApplications(PrintWriter w, List<Application> apps)
    {
        w.println("<applications>");
        for (Application app : apps) {
            w.print("  <application name='");
            w.print(XmlUtil.escapeXml(app.getName()));
            w.print("' description='");
            w.print(app.getDescription());
            w.print("' longDescription='");
            w.print(app.getLongDescription());
            w.print("' isHostService='");
            w.print(app.isHostService());
            w.print("' appJsUrl='");
            w.print(app.getAppJsUrl());
            w.println("'>");
            w.println("  </application>");
        }
        w.println("</applications>");
    }
}
