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
import com.metavize.mvvm.engine.PortalManagerPriv;
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
        PortalManagerPriv pm = (PortalManagerPriv)mctx.portalManager();

        HttpSession s = req.getSession();

        String user = req.getParameter("user");
        String password = req.getParameter("password");

        System.out.println("USER: " + user);
        System.out.println("PASSWORD: " + password);

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
                resp.getWriter().println("failure");
            } else {
                s.setAttribute("portal-login-key", plk);
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
