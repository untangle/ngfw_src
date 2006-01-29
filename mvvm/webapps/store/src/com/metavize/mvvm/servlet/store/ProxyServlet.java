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

package com.metavize.mvvm.servlet.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpParser;
import org.apache.commons.httpclient.StatusLine;
import org.apache.log4j.Logger;

public class ProxyServlet extends HttpServlet
{
    private static final String URI_BASE = "/joomla";

    private final Logger logger = Logger.getLogger(getClass());

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        InputStream sis = null;
        OutputStream sos = null;

        try {
            Socket s = new Socket();
            InetAddress addr = InetAddress.getByName("butters");
            s.connect(new InetSocketAddress(addr, 80));

            sis = s.getInputStream();
            sos = s.getOutputStream();

            StringBuilder sb = new StringBuilder();

            String pi = req.getPathInfo();
            String qs = req.getQueryString();

            String uri = URI_BASE + (null == pi ? "/" : pi) + "?" + (null == qs ? "" : qs);

            sb.append("GET " + uri + " HTTP/1.0\r\n");
            sb.append("Host: butters\r\n");
            sb.append("\r\n");
            sos.write(sb.toString().getBytes());
            copyStream(req.getInputStream(), sos);


            StatusLine sl = readStatusLine(sis);
            for (Header h : readHeader(sis)) {
                resp.addHeader(h.getName(), h.getValue());
            }

            copyStream(sis, resp.getOutputStream());
        } catch (UnknownHostException exn) {
            // XXX show page about this instead
            throw new ServletException("unknown host", exn);
        } catch (IOException exn) {
            // XXX show page about this instead
            throw new ServletException("unknown host", exn);
        } finally {
            if (null != sis) {
                try {
                    sis.close();
                } catch (IOException exn) {
                    logger.warn("could not close Socket InputStream");
                }
            }

            if (null != sos) {
                try {
                    sos.close();
                } catch (IOException exn) {
                    logger.warn("could not close Socket OutputStream");
                }
            }
        }

    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {

    }

    private void copyStream(InputStream is, OutputStream os)
        throws IOException
    {
        byte[] buf = new byte[4096];
        int i = 0;
        while (0 <= (i = is.read(buf))) {
            os.write(buf, 0, i);
        }

        os.flush();
    }

    private StatusLine readStatusLine(InputStream is)
        throws HttpException, IOException
    {
        String str = HttpParser.readLine(is, "US-ASCII");
        return new StatusLine(str);
    }

    private Header[] readHeader(InputStream is)
        throws IOException
    {
        return HttpParser.parseHeaders(is, "US-ASCII");
    }
}
