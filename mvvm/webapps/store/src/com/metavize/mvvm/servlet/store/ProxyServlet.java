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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
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
    private static final String STORE_HOST;
    private static final String URI_BASE;
    private static final String BASE_URL;

    static {
        String s = System.getProperty("mvvm.store.host");
        STORE_HOST = null == s ? "store.metavize.com" : s;
        s = System.getProperty("mvvm.store.uri");
        URI_BASE = null == s ? "/" : s;
        BASE_URL = "https://" + STORE_HOST + URI_BASE;
    }

    private final Logger logger = Logger.getLogger(getClass());

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        InputStream sis = null;
        OutputStream sos = null;

        try {
            SocketFactory ssf = SSLSocketFactory.getDefault();
            Socket s = ssf.createSocket();
            InetAddress addr = InetAddress.getByName(STORE_HOST);
            s.connect(new InetSocketAddress(addr, 443));
            sis = s.getInputStream();
            sos = s.getOutputStream();

            StringBuilder sb = new StringBuilder();

            String pi = req.getPathInfo();
            String qs = req.getQueryString();

            String uri = URI_BASE + (null == pi ? "" : pi) + "?" + (null == qs ? "" : qs);

            sb.append("GET " + uri + " HTTP/1.0\r\n");
            sb.append("Host: ").append(STORE_HOST).append("\r\n");
            sb.append("\r\n");
            sos.write(sb.toString().getBytes());
            copyStream(req.getInputStream(), sos);


            StatusLine sl = readStatusLine(sis);
            resp.setStatus(sl.getStatusCode(), sl.getReasonPhrase());

            boolean rewriteStream = false;
            for (Header h : readHeader(sis)) {
                if (h.getName().equalsIgnoreCase("content-type")) {
                    String v = h.getValue().toLowerCase();
                    if (v.startsWith("text/html")) {
                        rewriteStream = true;
                    }
                }
                resp.addHeader(h.getName(), h.getValue());
            }

            if (rewriteStream) {
                rewriteStream(sis, resp.getOutputStream());
            } else {
                copyStream(sis, resp.getOutputStream());
            }
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

    private void rewriteStream(InputStream is, OutputStream os)
        throws IOException
    {
        // XXX charset!
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os));

        int[] buf = new int[BASE_URL.length()];
        resetBuffer(buf);

        int i = 0;
        while (0 <= (buf[i] = r.read())) {
            i = (i + 1) % BASE_URL.length();
            if (matches(BASE_URL, buf, i)) {
                resetBuffer(buf);
                i = 0;
                w.append("./"); // XXX not correct, in general
            } else {
                if (0 <= buf[i]) {
                    w.append((char)buf[i]);
                }
            }
        }

        i = (i + 1) % BASE_URL.length();
        while (0 <= buf[i]) {
            w.append((char)buf[i]);
            i = (i + 1) % BASE_URL.length();
        }

        w.flush();
    }

    private void resetBuffer(int[] buf)
    {
        for (int i = 0; i < buf.length; i++) {
            buf[i] = -1;
        }
    }

    // matches a circular buffer
    private boolean matches(String str, int[] buf, int start)
    {
        if (str.length() != buf.length) {
            return false;
        }

        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) != (char)buf[(start + i) % buf.length]) {
                return false;
            }
        }

        return true;
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
