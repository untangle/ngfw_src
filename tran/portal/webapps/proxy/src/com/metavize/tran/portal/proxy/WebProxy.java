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
import java.io.PrintWriter;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.catalina.util.CookieTools;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class WebProxy extends HttpServlet
{
    private static final String HTML_READER = "org.htmlparser.sax.XMLReader";
    private static final String HTTP_CLIENT = "httpClient";

    private Logger logger = Logger.getLogger(getClass());

    @Override
    public void init() throws ServletException
    {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        HttpMethod method = new GetMethod(getUrl(req));
        doIt(req, resp, method);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        RawPostMethod method = new RawPostMethod(getUrl(req));

        try {
            method.setBodyStream(req.getContentType(), req.getInputStream(),
                                 req.getIntHeader("Content-Length"));
            doIt(req, resp, method);
        } catch (IOException exn) {
            logger.warn("could not process POST", exn);
        }
    }

    // private methods --------------------------------------------------------

    private void doIt(HttpServletRequest req, HttpServletResponse resp,
                      HttpMethod method)
        throws ServletException
    {
        HttpSession s = req.getSession();

        HttpClient httpClient = (HttpClient)s.getAttribute(HTTP_CLIENT);
        if (null == httpClient) {
            httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
            s.setAttribute(HTTP_CLIENT, httpClient);
        }

        String p = req.getPathInfo();
        // XXX if p invalid redirect to portal home?
        int i = p.indexOf('/', 1);
        String host = p.substring(1, 1 > i ? p.length() : i);
        String ctxPath = req.getContextPath();

        for (Enumeration e = req.getHeaderNames(); e.hasMoreElements(); ) {
            String k = (String)e.nextElement();
            if (k.equalsIgnoreCase("transfer-encoding")
                || k.equalsIgnoreCase("content-length")
                || k.equalsIgnoreCase("cookie")) {
                // skip
            } else if (k.equalsIgnoreCase("host")) {
                method.addRequestHeader("host", host);
            } else {
                for (Enumeration f = req.getHeaders(k); f.hasMoreElements(); ) {
                    String v = (String)f.nextElement();
                    method.addRequestHeader(k, v);
                }
            }
        }

        StringBuffer sb = new StringBuffer();
        Cookie[] cookies = req.getCookies();
        if (null == cookies) {
            cookies = new Cookie[0];
        }
        for (Cookie c : cookies) {
            //CookieTools.getCookieHeaderValue(c, sb);
            String kaka = CookieTools.getCookieHeaderName(c);
        }

        try {
            int rc = httpClient.executeMethod(method); // XXX release method
            resp.setStatus(rc);
        } catch (IOException exn) {
            logger.warn("could not make request", exn);
            try {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            } catch (IOException x) {
                throw new ServletException(x);
            }
            return;
        }

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
            } else if (name.equalsIgnoreCase("transfer-encoding")
                       || name.equalsIgnoreCase("content-length")) {
                // don't forward
            } else {
                resp.setHeader(name, value);
            }
        }

        InputStream is = null;

        try {
            is = method.getResponseBodyAsStream();

            if (rewriteStream) {
                PrintWriter w = null;
                try {
                    w = new PrintWriter(resp.getWriter());
                    rewriteStream(host, ctxPath, is, w);
                } finally {
                    if (null != w) {
                        w.close();
                    }
                }
            } else {
                OutputStream os = null;
                try {
                    os = resp.getOutputStream();
                    copyStream(is, os);
                } finally {
                    if (null != os) {
                        try {
                            os.close();
                        } catch (IOException exn) {
                            logger.warn("could not close OutputStream", exn);
                        }
                    }
                }
            }
        } catch (IOException exn) {
            logger.warn("could not stream", exn);
            // XXX what now?
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException exn) {
                    logger.warn("could not close InputStream", exn);
                }
            }
        }
    }

    private String getUrl(HttpServletRequest req)
    {
        String p = req.getPathInfo();
        String qs = req.getQueryString();

        return "http:/" + p + (null == qs ? "" : "?" + qs);
    }

    private void rewriteStream(String host, String ctxPath, InputStream is,
                               PrintWriter w)
        throws IOException
    {
        try {
            XMLReader xr = XMLReaderFactory.createXMLReader(HTML_READER);
            w = new PrintWriter(w);
            HtmlRewriter ch = new HtmlRewriter(w, ctxPath, host);
            xr.setDTDHandler(ch);
            xr.setContentHandler(ch);
            xr.setErrorHandler(ch);
            xr.parse(new InputSource(is));
        } catch (SAXException exn) {
            logger.warn("could not rewrite stream", exn);
            // XXX what now?
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
}
