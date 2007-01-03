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

package com.untangle.tran.portal.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmLocalContext;
import com.untangle.mvvm.NetworkManager;
import com.untangle.mvvm.networking.NetworkUtil;
import com.untangle.mvvm.portal.Application;
import com.untangle.mvvm.portal.Bookmark;
import com.untangle.mvvm.portal.LocalApplicationManager;
import com.untangle.mvvm.portal.LocalPortalManager;
import com.untangle.mvvm.portal.PortalHomeSettings;
import com.untangle.mvvm.portal.PortalLogin;
import com.untangle.mvvm.portal.PortalUser;
import com.untangle.mvvm.tran.IPaddr;
import org.apache.log4j.Logger;

public class ForwardServlet extends HttpServlet
{
    public static final int CONNECTION_TIMEOUT = 5000;
    private static final long TIMEOUT_MILLIS = 1200000L; // 20 min

    public static final String TARGET_HEADER = "Target";

    private MvvmLocalContext mvvmContext;
    private LocalPortalManager portalManager;
    private LocalApplicationManager appManager;
    private NetworkManager netManager;
    private Logger logger;

    // HttpServlet methods ----------------------------------------------------

    @Override
    public void init() throws ServletException
    {
        logger = Logger.getLogger(getClass());
        mvvmContext = MvvmContextFactory.context();
        portalManager = mvvmContext.portalManager();
        appManager = portalManager.applicationManager();
        netManager = mvvmContext.networkManager();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        String destHost;
        int destPort;
        int idleTime = 1200;
        HttpSession session = req.getSession();

        portalManager.incrementStatCounter(LocalPortalManager.FORWARD_COUNTER);

        PortalLogin pl = (PortalLogin)req.getUserPrincipal();

        if (null == pl) {
            logger.warn("no principal");
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
        PortalHomeSettings phs = portalManager.getPortalHomeSettings(pu);
        if (phs == null) {
            logger.warn("No portal home settings for " + pl);
        } else {
            idleTime = (int)(phs.getIdleTimeout() / 1000L);
        }

        try {
            Bookmark target = null;
            String targetStr = req.getHeader(TARGET_HEADER);
            if (targetStr == null)
                throw new ServletException("Missing target");

            long targetId;
            try {
                targetId = Long.parseLong(targetStr);
            } catch (NumberFormatException x) {
                throw new ServletException("Malformed target " + targetStr);
            }

            List<Bookmark> allBookmarks = portalManager.getAllBookmarks(pu);
            for (Iterator<Bookmark> iter = allBookmarks.iterator(); iter.hasNext();) {
                Bookmark bm = iter.next();
                if (bm.getId() == targetId) {
                    target = bm;
                    break;
                }
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

            if (logger.isInfoEnabled())
                logger.info("Opening forward for " + pl + " to " + destHost + ":" + destPort);

            InetSocketAddress isa = new InetSocketAddress(destHost, destPort);
            InetAddress addr = isa.getAddress();
            if (null == addr) {
                String msg = "Unable to resolve hostname: " + destHost;
                logger.warn(msg);
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
                return;
            } else if (netManager.isAddressLocal(new IPaddr(addr))) {
                String msg = "Unable to forward to local address: " + destHost;
                logger.warn(msg);
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
                return;
            }

            session.setMaxInactiveInterval(idleTime);

            resp.setBufferSize(0);
            // Turn on chunking:
            resp.addHeader("Connection", "keep-alive");
            resp.flushBuffer();        // Ensure response line/headers get there.

            Socket s = new Socket();
            try {
                s.connect(isa, CONNECTION_TIMEOUT);
            } catch (NoRouteToHostException exn) {
                String msg = "No route to host";
                logger.warn(msg, exn);
                resp.sendError(HttpServletResponse.SC_GATEWAY_TIMEOUT, msg);
                return;
            } catch (SocketTimeoutException exn) {
                String msg = "socket timed out";
                logger.warn(msg, exn);
                resp.sendError(HttpServletResponse.SC_GATEWAY_TIMEOUT, msg);
                return;
            }

            InputStream his =  req.getInputStream();
            OutputStream hos = resp.getOutputStream();
            InputStream sis =  s.getInputStream();
            OutputStream sos = s.getOutputStream();

            session.setMaxInactiveInterval(idleTime);

            Worker w1 = new Worker(his, sos);
            Thread t1 = mvvmContext.newThread(w1);
            t1.setName("forward reader " + t1.getName());
            Worker w2 = new Worker(sis, hos);
            Thread t2 = mvvmContext.newThread(w2);
            t2.setName("forward writer " + t2.getName());

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
            throw new ServletException("Unable to forward", exn);
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

                long lastTx = System.currentTimeMillis();

                while (true) {
                    try {
                        if (logger.isDebugEnabled()) {
                            logger.debug(Thread.currentThread().getName()
                                         + " about to read");
                        }
                        i = is.read(buf);
                        if (logger.isDebugEnabled()) {
                            logger.debug(Thread.currentThread().getName()
                                         + " got " + i);
                        }
                    } catch (SocketTimeoutException exn) {
                        long t = System.currentTimeMillis();
                        if (t - lastTx > TIMEOUT_MILLIS) {
                            logger.debug(Thread.currentThread().getName()
                                         + " timed out");
                            break;
                        } else {
                            // This is expected with Tomcat sockets,
                            // just gives us a chance to see if the
                            // session timeout has expired or
                            // whatever. XXX
                            continue;
                        }
                    }
                    if (i < 0) {
                        break;
                    }
                    lastTx = System.currentTimeMillis();
                    os.write(buf, 0, i);
                    os.flush();
                }
            } catch (IOException exn) {
                logger.warn("could not read or write", exn);
            } finally {
                try {
                    os.close();
                } catch (IOException exn) {
                    logger.warn("exception closing os", exn);
                }
                try {
                    is.close();
                } catch (IOException exn) {
                    logger.warn("exception closing is", exn);
                }
            }
        }
    }
}


