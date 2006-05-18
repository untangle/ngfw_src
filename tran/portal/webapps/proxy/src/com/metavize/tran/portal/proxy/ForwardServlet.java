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

package com.metavize.tran.portal.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.portal.Application;
import com.metavize.mvvm.portal.Bookmark;
import com.metavize.mvvm.portal.LocalApplicationManager;
import com.metavize.mvvm.portal.LocalPortalManager;
import com.metavize.mvvm.portal.PortalLogin;
import com.metavize.mvvm.portal.PortalUser;
import org.apache.log4j.Logger;

public class ForwardServlet extends HttpServlet
{
    public static final String TARGET_HEADER = "Target";

    private MvvmLocalContext mvvmContext;
    private LocalPortalManager portalManager;
    private LocalApplicationManager appManager;
    private Logger logger;

    // HttpServlet methods ----------------------------------------------------

    @Override
    public void init() throws ServletException
    {
        logger = Logger.getLogger(getClass());
        mvvmContext = MvvmContextFactory.context();
        portalManager = mvvmContext.portalManager();
        appManager = portalManager.applicationManager();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        String destHost;
        int destPort;
        HttpSession session = req.getSession();

        PortalLogin pl = (PortalLogin)req.getUserPrincipal();

        if (null == pl) {
            System.out.println("NO PRINCIPAL! " + this);
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

        try {
            Bookmark target = null;
            String targetStr = req.getHeader(TARGET_HEADER);
            if (targetStr == null)
                throw new ServletException("Missing target");
            try {
                long targetId = Long.parseLong(targetStr);
                List<Bookmark> allBookmarks = portalManager.getAllBookmarks(pu);
                for (Iterator<Bookmark> iter = allBookmarks.iterator(); iter.hasNext();) {
                    Bookmark bm = iter.next();
                    if (bm.getId() == targetId) {
                        target = bm;
                        break;
                    }
                }
            } catch (NumberFormatException x) {
                throw new ServletException("Malformed target " + targetStr);
            }
            if (target == null)
                throw new ServletException("Target not found");

            Application app = appManager.getApplication(target.getApplicationName());
            if (app == null)
                throw new ServletException("Target application unknown: " +
                                           target.getApplicationName());
            Application.Destinator dest = app.getDestinator();
            destHost = dest.getDestinationHost(target);
            if (destHost == null)
                throw new ServletException("Target does not contain destination host");
            destPort = dest.getDestinationPort(target);

            session.setMaxInactiveInterval(1200);            // XXX Use user's timeout

            InputStream his = req.getInputStream();
            OutputStream hos = resp.getOutputStream();
            Socket s = new Socket(destHost, destPort);
            InputStream sis = s.getInputStream();
            OutputStream sos = s.getOutputStream();

            resp.setBufferSize(0);
            resp.flushBuffer();        // Ensure response line/headers get there.

            session.setMaxInactiveInterval(1200);            // XXX Use user's timeout

            Worker w1 = new Worker(his, sos);
            Thread t1 = mvvmContext.newThread(w1);
            Worker w2 = new Worker(sis, hos);
            Thread t2 = mvvmContext.newThread(w2);

            t1.start();
            t2.start();

            while (t1.isAlive() || t2.isAlive()) {
                try {
                    t1.join();
                    t2.join();
                } catch (InterruptedException exn) {
                    // XXX XXX need to be able to shut down
                }
            }
        } catch (IOException exn) {
            logger.error("could not get stream", exn);
        }
    }

    private class Worker implements Runnable
    {
        private final InputStream is;
        private final OutputStream os;

        Worker(InputStream is, OutputStream os)
        {
            this.is = is;
            this.os = os;
        }

        public void run()
        {
            try {
                byte[] buf = new byte[1024];
                int i;
                while (true) {
                    try {
                        i = is.read(buf);
                    } catch (SocketTimeoutException exn) {
                        // This is expected with Tomcat sockets, just gives us a chance
                        // to see if the session timeout has expired or whatever. XXX
                        continue;
                    }
                    if (i < 0)
                        break;
                    os.write(buf, 0, i);
                    os.flush();
                }
            } catch (IOException exn) {
                logger.warn("could not read or write", exn);
            } finally {
                try {
                    os.close();
                    is.close();
                } catch (IOException exn) {
                    logger.error("exception closing", exn);
                }
            }
        }
    }
}
