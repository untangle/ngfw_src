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

package com.metavize.tran.portal;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.engine.PortalManagerPriv;
import com.metavize.mvvm.portal.ApplicationManager;
import com.metavize.mvvm.portal.PortalLogin;
import com.metavize.mvvm.portal.PortalLoginKey;
import com.metavize.mvvm.portal.PortalUser;
import com.metavize.mvvm.util.XmlUtil;
import org.apache.log4j.Logger;

public class ApplicationServlet extends HttpServlet
{
    private Logger logger;
    private PortalManagerPriv portalManager;
    private ApplicationManager appManager;

    // HttpServlet methods ----------------------------------------------------

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        System.out.println("BookmarkServlet doGet");
        HttpSession s = req.getSession();

        resp.setContentType("text/xml");
        resp.addHeader("Cache-Control", "no-cache");

        PortalLoginKey plk = (PortalLoginKey)s.getAttribute(PortalManagerPriv.PORTAL_LOGIN_KEY);
        if (null == plk) {
            logger.info("not logged in: " + plk);
            try {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch (IOException exn) {
                logger.warn("could not send error", exn);
            }
            return;
        }

        PortalLogin pl = portalManager.getLogin(plk);

        if (null == pl) {
            logger.info("login timeout: " + plk);
            try {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch (IOException exn) {
                logger.warn("could not send error", exn);
            }
            return;
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
                String[] apps = appManager.getApplicationNames();
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
        portalManager = (PortalManagerPriv)mctx.portalManager();
        appManager = portalManager.applicationManager();
    }

    // private methods --------------------------------------------------------

    private void emitApplications(PrintWriter w, String[] apps)
    {
        w.println("<applications>");
        for (String app : apps) {
            w.print("  <application name='");
            w.print(XmlUtil.escapeXml(app));
            w.println("'/>");
        }
        w.println("</applications>");
    }
}
