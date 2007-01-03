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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmLocalContext;
import com.untangle.mvvm.portal.Application;
import com.untangle.mvvm.portal.Bookmark;
import com.untangle.mvvm.portal.LocalApplicationManager;
import com.untangle.mvvm.portal.LocalPortalManager;
import com.untangle.mvvm.portal.PortalGroup;
import com.untangle.mvvm.portal.PortalLogin;
import com.untangle.mvvm.portal.PortalUser;
import com.untangle.mvvm.util.XmlUtil;
import org.apache.log4j.Logger;

public class BookmarkServlet extends HttpServlet
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
                PrintWriter w = resp.getWriter();
                w.println("<bookmarks>");

                List<Bookmark> bms = pu.getBookmarks();
                emitBookmarks(w, bms, "user");

                PortalGroup userGroup = pu.getPortalGroup();
                if (userGroup != null) {
                    bms = userGroup.getBookmarks();
                    emitBookmarks(w, bms, "group");
                }

                bms = portalManager.getPortalSettings().getGlobal()
                    .getBookmarks();
                emitBookmarks(w, bms, "global");
                w.println("</bookmarks>");
            } else if (command.equals("add")) {
                String name = req.getParameter("name");
                String appName = req.getParameter("app");
                Application app = appManager.getApplication(appName);
                String target = req.getParameter("target");
                portalManager.addUserBookmark(pu, name, app, target);
            } else if (command.equals("edit")) {
                String idStr = req.getParameter("id");
                Long id = Long.valueOf(idStr);
                String name = req.getParameter("name");
                String appName = req.getParameter("app");
                Application app = appManager.getApplication(appName);
                String target = req.getParameter("target");
                portalManager.editUserBookmark(pu, id, name, app, target);
            } else if (command.equals("rm")) {
                Set<Long> ids = new HashSet<Long>();
                for (String idStr : req.getParameterValues("id")) {
                    Long id = Long.parseLong(idStr);
                    ids.add(id);
                }
                portalManager.removeUserBookmarks(pu, ids);
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

    private void emitBookmarks(PrintWriter w, List<Bookmark> bookmarks,
                               String type)
    {
        for (Bookmark bm : bookmarks) {
            String name = bm.getName();
            if (null == name) {
                name = "Name";
            }

            String appName = bm.getApplicationName();
            if (null == appName) {
                appName = "Application";
            }

            String target = bm.getTarget();
            if (null == target) {
                target = "Target";
            }

            w.print("  <bookmark id='");
            w.print(XmlUtil.escapeXml(bm.getId().toString()));
            w.print("' name='");
            w.print(XmlUtil.escapeXml(name));
            w.print("' app='");
            w.print(XmlUtil.escapeXml(appName));
            w.print("' target='");
            w.print(XmlUtil.escapeXml(target));
            w.print("' type='");
            w.print(XmlUtil.escapeXml(type));
            w.println("'/>");
        }
    }
}
