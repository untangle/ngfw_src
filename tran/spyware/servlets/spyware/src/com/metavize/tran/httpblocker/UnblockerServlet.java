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

package com.metavize.tran.httpblocker;

import java.io.IOException;
import javax.security.auth.login.FailedLoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.client.MvvmConnectException;
import com.metavize.mvvm.client.MvvmRemoteContext;
import com.metavize.mvvm.client.MvvmRemoteContextFactory;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformManager;

public class UnblockerServlet extends HttpServlet
{
    // HttpServlet methods ----------------------------------------------------

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        resp.setContentType("text/xml");
        resp.addHeader("Cache-Control", "no-cache");

        String nonce = req.getParameter("nonce");
        String tidStr = req.getParameter("tid");
        boolean global = Boolean.parseBoolean(req.getParameter("global"));

        try {
            MvvmRemoteContext ctx = MvvmRemoteContextFactory.factory().systemLogin(0, Thread.currentThread().getContextClassLoader());
            TransformManager tman = ctx.transformManager();
            Tid tid = new Tid(Long.parseLong(tidStr));
            TransformContext tctx = tman.transformContext(tid);
            HttpBlocker tran = (HttpBlocker)tctx.transform();

            if (tran.unblockSite(nonce, global)) {
                resp.getOutputStream().println("<success/>");
            } else {
                resp.getOutputStream().println("<failure/>");
            }
        } catch (FailedLoginException exn) {
            throw new ServletException(exn);
        } catch (MvvmConnectException exn) {
            throw new ServletException(exn);
        } catch (IOException exn) {
            throw new ServletException(exn);
        } finally {
            MvvmRemoteContextFactory.factory().logout();
        }
    }
}

