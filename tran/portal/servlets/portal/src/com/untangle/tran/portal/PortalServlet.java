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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmLocalContext;
import com.untangle.mvvm.portal.LocalApplicationManager;
import com.untangle.mvvm.portal.LocalPortalManager;
import com.untangle.mvvm.portal.PortalHomeSettings;
import com.untangle.mvvm.portal.PortalLogin;
import com.untangle.mvvm.portal.PortalUser;
import com.untangle.mvvm.util.XmlUtil;
import org.apache.log4j.Logger;

public class PortalServlet extends HttpServlet
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
            logger.warn("no principal" + this);
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
            PrintWriter writer = resp.getWriter();

            if (command.equals("info")) {
                PortalHomeSettings phs = portalManager.getPortalHomeSettings(pu);
                String title = phs.getHomePageTitle();
                String username = pu.getUid();
                String motd = phs.getHomePageText();
                String bookmarkTitle = phs.getBookmarkTableTitle();
                boolean showApps = phs.isShowExploder();
                boolean showBookmarks = phs.isShowBookmarks();
                boolean showAddBookmark = phs.isShowAddBookmark();
                writer.print("<page-info title='");
                writer.print(XmlUtil.escapeXml(title));
                writer.print("' username='");
                writer.print(XmlUtil.escapeXml(username));
                writer.print("' motd='");
                writer.print(XmlUtil.escapeXml(motd));
                writer.print("' bookmarkTitle='");
                writer.print(XmlUtil.escapeXml(bookmarkTitle));
                writer.print("' showApps='");
                writer.print(showApps);
                writer.print("' showBookmarks='");
                writer.print(showBookmarks);
                writer.print("' showAddBookmark='");
                writer.print(showAddBookmark);
                writer.println("'/>");
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
}
