/*
 * $HeadURL$
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

package com.untangle.uvm.servlet.store;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.toolbox.MackageDesc;
import com.untangle.uvm.toolbox.RemoteToolboxManager;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

/**
 * A reverse proxy that makes our online store appear to be hosted on
 * the Untangle appliance. Implemented by replacing all strings with a
 * base URL with the base URL of the appliance.
 *
 * XXX we should reuse WebProxy from the Portal
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class ProxyServlet extends HttpServlet
{
    private static final String STORE_HOST;
    private static final String COOKIE_DOMAIN;
    private static final String URI_BASE;
    private static final String BASE_URL;
    private static final Map<Character, Integer> OCCURANCE;

    private static final String INST_COOKIE = "instCookie";

    static {
        String s = System.getProperty("uvm.store.host");
        STORE_HOST = null == s ? "library.untangle.com" : s;
        COOKIE_DOMAIN = STORE_HOST;
        s = System.getProperty("uvm.store.uri");
        URI_BASE = null == s ? "" : s;
        BASE_URL = "https://" + STORE_HOST + URI_BASE;

        Map<Character, Integer> m = new HashMap<Character, Integer>();

        for (int i = 0; i < BASE_URL.length(); i++) {
            m.put(BASE_URL.charAt(i), i);
        }

        OCCURANCE = Collections.unmodifiableMap(m);
    }

    private final HttpClientCache clientCache;
    private final Logger logger = Logger.getLogger(getClass());

    // constructors -----------------------------------------------------------

    public ProxyServlet()
    {
        clientCache = new HttpClientCache(COOKIE_DOMAIN);
    }

    // HttpServlet methods ----------------------------------------------------

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        String url = getUrl(req);
        HttpMethod get = new GetMethod(url);
        get.setFollowRedirects(false);

        doIt(get, req, resp);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        String url = getUrl(req);
        RawPostMethod post = new RawPostMethod(url);
        post.setFollowRedirects(false);
        try {
            post.setBodyStream(req.getContentType(), req.getInputStream(),
                               req.getIntHeader("Content-Length"));
            doIt(post, req, resp);
        } catch (IOException exn) {
            logger.warn("could not do post", exn);
        }
    }

    private void doIt(HttpMethod method, HttpServletRequest req,
                      HttpServletResponse resp)
        throws ServletException
    {
        LocalUvmContext ctx = LocalUvmContextFactory.context();

        HttpSession s = req.getSession();

        HttpClient httpClient = clientCache.getClient(req, resp);
        HttpState state = httpClient.getState();

        RemoteToolboxManager tool = ctx.toolboxManager();
        tool.installed();
        StringBuilder sb = new StringBuilder();
        for (MackageDesc md : tool.installed()) {
            MackageDesc.Type type = md.getType();
            /* ignore packages that don't have an untangle type */
            if (null == type || MackageDesc.Type.UNKNOWN == type) {
                continue;
            }
            sb.append(md.getName());
            sb.append("=");
            sb.append(md.getInstalledVersion());
            sb.append(",");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }

        Cookie instCookie = (Cookie)s.getAttribute(INST_COOKIE);
        if (null == instCookie) {
            instCookie = new Cookie(COOKIE_DOMAIN, "installed", sb.toString(), "/", -1, false);
            state.addCookie(instCookie);
            s.setAttribute(INST_COOKIE, instCookie);
        } else {
            instCookie.setValue(sb.toString());
        }

        InputStream is = null;
        OutputStream os = null;

        try {
            copyHeaders(req, method);
            int rc = httpClient.executeMethod(method);
            is = method.getResponseBodyAsStream();

            StatusLine sl = method.getStatusLine();
            resp.setStatus(sl.getStatusCode());
            copyHeaders(method, resp, req);

            boolean rewriteStream = false;
            for (Header h : method.getResponseHeaders()) {
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

            os = resp.getOutputStream();
            if (rewriteStream) {
                rewriteStream(is, os);
            } else {
                copyStream(is, os);
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

    private String getUrl(HttpServletRequest req)
    {
        String pi = req.getPathInfo();
        String qs = req.getQueryString();

        return BASE_URL + (null == pi ? "" : pi)
            + (null == qs ? "" : ("?" + qs));
    }

    private void copyStream(InputStream is, OutputStream os)
        throws IOException
    {
        if (null != is) {
            byte[] buf = new byte[4096];
            int i = 0;
            while (0 <= (i = is.read(buf))) {
                os.write(buf, 0, i);
            }
        }

        os.flush();
    }

    private int rewriteStream(InputStream is, OutputStream os)
        throws IOException
    {
        int count = 0;

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
                    return count;
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
                count++;
                w.append("."); // XXX not correct in general
                i = 0;
            } else {
                Integer k = OCCURANCE.get(buf[(offset + j) % buf.length]);

                if (null == k) {
                    for (int m = 0; m < j + 1; m++) {
                        w.write(buf[(offset + m) % buf.length]);
                    }
                    offset = (offset + (j + 1)) % buf.length;
                    i = buf.length - (j + 1);
                } else {
                    int l = Math.max(j - k, 1);

                    for (int m = 0; m < l; m++) {
                        w.write(buf[(offset + m) % buf.length]);
                    }
                    offset = (offset + l) % buf.length;
                    i = buf.length - l;
                }
            }
        }
    }

    private void copyHeaders(HttpServletRequest req, HttpMethod method)
    {
        for (Enumeration e = req.getHeaderNames(); e.hasMoreElements(); ) {
            String k = (String)e.nextElement();
            if (k.equalsIgnoreCase("transfer-encoding")
                || k.equalsIgnoreCase("content-length")
                || k.equalsIgnoreCase("cookie")) {
                // skip
            } else if (k.equalsIgnoreCase("host")) {
                method.addRequestHeader("Host", STORE_HOST);
            } else if (k.equalsIgnoreCase("referer")) {
                method.addRequestHeader("Referer", BASE_URL);
            } else {
                for (Enumeration f = req.getHeaders(k); f.hasMoreElements(); ) {
                    String v = (String)f.nextElement();
                    method.addRequestHeader(k, v);
                }
            }
        }
    }

    private void copyHeaders(HttpMethod method, HttpServletResponse resp,
                             HttpServletRequest req)
    {
        for (Header h : method.getResponseHeaders()) {
            String name = h.getName();
            String value = h.getValue();

            if (name.equalsIgnoreCase("content-type")) {
                resp.setContentType(value);
            } else if (name.equalsIgnoreCase("transfer-encoding")
                       || name.equalsIgnoreCase("content-length")
                       || name.equalsIgnoreCase("set-cookie")) {
                // don't forward
            } else if (name.equalsIgnoreCase("location")
                       || name.equalsIgnoreCase("content-location")) {
                resp.setHeader(name, value);
            } else {
                resp.setHeader(name, value);
            }
        }
    }

    // test -------------------------------------------------------------------

    public static void main(String[] args) throws Exception
    {
        final int numTimes = 10000;

        final PipedOutputStream os = new PipedOutputStream();
        PipedInputStream is = new PipedInputStream(os);

        Runnable writer = new Runnable()
            {
                public void run()
                {
                    try {
                        Random r = new Random();
                        for (int i = 0; i < numTimes; i++) {
                            int l = r.nextInt() % 100;
                            for (int j = 0; j < l; j++) {
                                os.write((byte)r.nextInt() % 256);
                            }

                            os.write(BASE_URL.toString().getBytes());
                        }

                        os.close();
                    } catch (Exception exn) {
                        exn.printStackTrace();
                    }
                }
            };

        FileOutputStream fos = new FileOutputStream("/dev/null");


        Thread t = new Thread(writer);
        t.start();

        ProxyServlet ps = new ProxyServlet();
        int i = ps.rewriteStream(is, fos);

        System.out.println("numTimes: " + numTimes + " I: " + i);
    }
}
