/*
 * $HeadURL: svn://chef/work/src/uvm-lib/servlets/onlinestore/src/com/untangle/uvm/servlet/store/HttpClientCache.java $
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.servlet.alpaca;

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

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import org.apache.log4j.Logger;

public class ForwardServlet extends HttpServlet
{
    public static final int CONNECTION_TIMEOUT = 5000;
    private static final long TIMEOUT_MILLIS = 1200000L; // 20 min

    private static final InetSocketAddress ALPACA_LOC
        = new InetSocketAddress("localhost", 3000);

    public static final String TARGET_HEADER = "Target";

    private LocalUvmContext uvmContext;

    private Logger logger;

    // HttpServlet methods ----------------------------------------------------

    @Override
    public void init() throws ServletException
    {
        uvmContext = LocalUvmContextFactory.context();
        logger = Logger.getLogger(getClass());
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        int idleTime = 1200;
        HttpSession session = req.getSession();

        try {
            session.setMaxInactiveInterval(idleTime);

            resp.setBufferSize(0);
            // Turn on chunking:
            resp.addHeader("Connection", "keep-alive");
            resp.flushBuffer();        // Ensure response line/headers get there.

            Socket s = new Socket();
            try {
                s.connect(ALPACA_LOC, CONNECTION_TIMEOUT);
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
            Thread t1 = uvmContext.newThread(w1);
            t1.setName("forward reader " + t1.getName());
            Worker w2 = new Worker(sis, hos);
            Thread t2 = uvmContext.newThread(w2);
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
                logger.warn(Thread.currentThread()
                            + " could not read or write", exn);
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
