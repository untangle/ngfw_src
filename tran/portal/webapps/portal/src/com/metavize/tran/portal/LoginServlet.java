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
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.portal.LocalPortalManager;
import com.metavize.mvvm.portal.PortalLoginKey;
import org.apache.log4j.Logger;

public class LoginServlet extends HttpServlet
{
    private Logger logger;

    // HttpServlet methods ----------------------------------------------------

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        MvvmLocalContext mctx = MvvmContextFactory.context();
        LocalPortalManager pm = mctx.portalManager();

        HttpSession s = req.getSession();

        String user = req.getParameter("user");
        String password = req.getParameter("password");

        String remote = req.getRemoteAddr();

        InetAddress addr;
        try {
            addr = InetAddress.getByName(remote);
        } catch (UnknownHostException exn) {
            addr = null;
        }

        PortalLoginKey plk = pm.login(user, password, addr);

        try {
            if (null == plk) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            } else {
                s.setAttribute(LocalPortalManager.PORTAL_LOGIN_KEY, plk);
                resp.getWriter().println("success");
            }
        } catch (IOException exn) {
            logger.warn("could not write response", exn);
        }
    }

    @Override
    public void init() throws ServletException
    {
        logger = Logger.getLogger(getClass());
    }
}
