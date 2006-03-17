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
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

public class ProxyServlet extends HttpServlet
{
    private static final String STORE_HOST;
    private static final String URI_BASE;
    private static final String BASE_URL;
    private static final Map<Character, Integer> BASE_MAP;

    private final HttpClient httpClient;

    static {
        String s = System.getProperty("mvvm.store.host");
        STORE_HOST = null == s ? "store.metavize.com" : s;
        s = System.getProperty("mvvm.store.uri");
        URI_BASE = null == s ? "/" : s;
        BASE_URL = "https://" + STORE_HOST + URI_BASE;

        Map<Character, Integer> m = new HashMap<Character, Integer>();
        for (int i = 0; i < BASE_URL.length(); i++) {
            m.put(BASE_URL.charAt(i), i);
        }

        BASE_MAP = Collections.unmodifiableMap(m);
    }

    private final Logger logger = Logger.getLogger(getClass());

    // constructors -----------------------------------------------------------

    public ProxyServlet()
    {
        httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
    }

    // HttpServlet methods ----------------------------------------------------

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        InputStream is = null;
        InputStream os = null;

        try {
            String pi = req.getPathInfo();
            String qs = req.getQueryString();

            String url = BASE_URL + (null == pi ? "" : pi) + "?"
                + (null == qs ? "" : qs);
            HttpMethod get = new GetMethod(url);
            int rc = httpClient.executeMethod(get);
            is = get.getResponseBodyAsStream();
            StatusLine sl = get.getStatusLine();
            resp.setStatus(sl.getStatusCode());

            boolean rewriteStream = false;
            for (Header h : get.getResponseHeaders()) {
                String name = h.getName();
                String value = h.getValue();

                if (name.equalsIgnoreCase("content-type")) {
                    resp.setContentType(value);

                    String v = value.toLowerCase();
                    if (v.startsWith("text/html")) {
                        rewriteStream = true;
                    }
                } else if (name.equalsIgnoreCase("date")) {
                    resp.setHeader(name, value);
                } else if (name.equalsIgnoreCase("etag")) {
                    resp.setHeader(name, value);
                } else if (name.equalsIgnoreCase("last-modified")) {
                    resp.setHeader(name, value);
                }
            }

            if (rewriteStream) {
                rewriteStream(is, resp.getOutputStream());
            } else {
                copyStream(is, resp.getOutputStream());
            }

        } catch (UnknownHostException exn) {
            // XXX show page about this instead
            throw new ServletException("unknown host", exn);
        } catch (IOException exn) {
            // XXX show page about this instead
            throw new ServletException("unknown host", exn);
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException exn) {
                    logger.warn("could not close Socket InputStream");
                }
            }

            if (null != os) {
                try {
                    os.close();
                } catch (IOException exn) {
                    logger.warn("could not close Socket OutputStream");
                }
            }
        }
    }

    protected void doHead(HttpServletRequest req, HttpServletResponse resp)
    {
        try {
            logger.warn("XXX we don't support head");
            resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
        } catch (IOException exn) {
            logger.warn("could not do head", exn);
        }
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

        char[] buf = new char[BASE_URL.length()];

        int offset = 0;
        int i = 0;

        while (true) {
            for ( ; i < buf.length; i++) {
                int c = r.read();
                if (0 > c) {
                    for (int m = 0; m < i; m++) {
                        w.write(buf[(offset + m) % buf.length]);
                    }
                    w.flush();
                    return;
                } else {
                    buf[(offset + i) % buf.length] = (char)c;
                }
            }

            int j;
            for (j = buf.length - 1; 0 <= j; j--) {
                if (buf[(offset + j) % buf.length] != BASE_URL.charAt(j)) {
                    break;
                }
            }

            if (-1 == j) {
                w.append("./"); // XXX not correct in general
                i = 0;
            } else {
                Integer k = BASE_MAP.get(buf[(offset + j) % buf.length]);

                if (null == k) {
                    for (int m = 0; m < j + 1; m++) {
                        w.write(buf[(offset + m) % buf.length]);
                    }
                    offset = (offset + (j + 1)) % buf.length;
                    i = buf.length - (j + 1);
                } else {
                    int l = j - k;
                    assert 0 != l;

                    if (0 > l) {
                        for (int m = 0; m < j + 1; m++) {
                            w.write(buf[(offset + m) % buf.length]);
                        }
                        offset = (offset + j + 1) % buf.length;
                        i = buf.length - (j + 1);
                    } else {
                        for (int m = 0; m < l; m++) {
                            w.write(buf[(offset + m) % buf.length]);
                        }
                        offset = (offset + l) % buf.length;
                        i = buf.length - l;
                    }
                }
            }
        }
    }
}
