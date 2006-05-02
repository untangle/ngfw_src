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
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.engine.PortalManagerPriv;
import com.metavize.mvvm.portal.Bookmark;
import com.metavize.mvvm.portal.PortalLogin;
import com.metavize.mvvm.portal.PortalLoginKey;
import com.metavize.mvvm.portal.PortalUser;
import com.metavize.mvvm.util.XmlUtil;
import org.apache.log4j.Logger;

public class BookmarkServlet extends HttpServlet
{
    private Logger logger;

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

        MvvmLocalContext mctx = MvvmContextFactory.context();
        PortalManagerPriv pm = (PortalManagerPriv)mctx.portalManager();

        PortalLogin pl = pm.getLogin(plk);

        if (null == pl) {
            logger.info("login timeout: " + plk);
            try {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch (IOException exn) {
                logger.warn("could not send error", exn);
            }
            return;
        }

        PortalUser pu = pm.getUser(pl.getUser());

        if (null == pu) {
            logger.warn("no portal user for login: " + pl.getUser());
            try {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch (IOException exn) {
                logger.warn("could not send error", exn);
            }
            return;
        }

        List<Bookmark> bms = pm.getAllBookmarks(pu);

        try {
            emitBookmarks(resp.getWriter(), bms);
        } catch (IOException exn) {
            logger.warn("could not write bookmarks", exn);
        }
    }

    @Override
    public void init() throws ServletException
    {
        logger = Logger.getLogger(getClass());
    }

    // private methods --------------------------------------------------------

    private void emitBookmarks(PrintWriter w, List<Bookmark> bookmarks)
    {
        w.println("<bookmarks>");
        for (Bookmark bm : bookmarks) {
            w.print("  <bookmark name='");
            w.print(XmlUtil.escapeXml(bm.getName()));
            w.print("' app='");
            w.print(XmlUtil.escapeXml(bm.getApplicationName()));
            w.print("' target='");
            w.print(XmlUtil.escapeXml(bm.getTarget()));
            w.println("'/>");
        }
        w.println("</bookmarks>");
    }
}
