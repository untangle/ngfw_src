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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class ForwardServlet extends HttpServlet
{
    private final Logger logger = Logger.getLogger(getClass());

    // HttpServlet methods ----------------------------------------------------

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        try {
            InputStream his = req.getInputStream();
            OutputStream hos = resp.getOutputStream();

            Socket s = new Socket("bebe", 7);
            InputStream sis = s.getInputStream();
            OutputStream sos = s.getOutputStream();

            Worker w1 = new Worker(his, sos);
            Thread t1 = new Thread(w1);
            Worker w2 = new Worker(sis, hos);
            Thread t2 = new Thread(w2);

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
            logger.warn("could not get stream", exn);
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
                while (0 <= (i = is.read(buf))) {
                    os.write(buf, 0, i);
                    os.flush();
                }
            } catch (IOException exn) {
                logger.warn("could not read or write", exn);
            }
        }
    }
}
