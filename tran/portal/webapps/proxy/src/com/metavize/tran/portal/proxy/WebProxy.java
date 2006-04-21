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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.catalina.util.CookieTools;
import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.fileupload.ParameterParser;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.htmlparser.Node;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.lexer.Page;
import org.htmlparser.util.ParserException;
import org.xml.sax.XMLReader;

public class WebProxy extends HttpServlet
{
    private static final byte[] CRLF = "\r\n".getBytes();
    private static final byte[] DASH_DASH = "--".getBytes();
    private static final String HTML_READER = "org.htmlparser.sax.XMLReader";
    private static final String HTTP_CLIENT = "httpClient";

    private static final Pattern CONTENT_TYPE_PATTERN;

    static {
        try {
            CONTENT_TYPE_PATTERN = Pattern.compile("^Content-Type:\\s*(.*)$",
                                                   Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        } catch (PatternSyntaxException exn) {
            throw new RuntimeException("could not compile regex", exn);
        }
    }

    private Logger logger;

    @Override
    public void init() throws ServletException
    {
        logger = Logger.getLogger(getClass());
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

        String contentType = "";

        for (Header h : method.getResponseHeaders()) {
            String name = h.getName();
            String value = h.getValue();

            if (name.equalsIgnoreCase("content-type")) {
                resp.setContentType(value);
                contentType = value;
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

            String ct = contentType.toLowerCase();

            if (ct.startsWith("text/html")) {
                PrintWriter w = null;
                try {
                    w = new PrintWriter(resp.getWriter());
                    rewriteStream(is, w, ctxPath, host);
                } catch (ParserException exn) {
                    logger.warn("could not parse html", exn);
                } finally {
                    if (null != w) {
                        w.close();
                    }
                }
            } else if (ct.startsWith("multipart/")) {
                OutputStream os = null;
                try {
                    os = resp.getOutputStream();
                    handleMultipart(contentType, is, os, ctxPath, host);
                } finally {
                    if (null != os) {
                        try {
                            os.close();
                        } catch (IOException exn) {
                            logger.warn("could not close OutputStream", exn);
                        }
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

    private void handleMultipart(String contentType,
                                 InputStream is, OutputStream os,
                                 String ctxPath, String host)
        throws IOException
    {
        ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);
        Map params = parser.parse(contentType, ';');
        String b = (String)params.get("boundary");
        if (null == b) {
            logger.warn("multipart without boundary");
            copyStream(is, os);
        } else {
            byte[] boundary = b.getBytes();
            MultipartStream mps = new MultipartStream(is, boundary);
            mps.readBodyData(os);
            while (mps.readBoundary()) {
                os.write(CRLF);
                os.write(DASH_DASH);
                os.write(boundary);
                os.write(CRLF);
                String headers = mps.readHeaders();
                os.write(headers.getBytes());
                Matcher m = CONTENT_TYPE_PATTERN.matcher(headers);
                if (m.find()) {
                    String ct = m.group(1);
                    if (ct.toLowerCase().startsWith("text/html")) {
                        // XXX get charset from header!
                        PrintWriter w = new PrintWriter(os);
                        rewriteStream(mps, w, ctxPath, host);
                    } else {
                        mps.readBodyData(os);
                    }
                } else {
                    mps.readBodyData(os);
                }
            }

            os.write(CRLF);
            os.write(DASH_DASH);
            os.write(boundary);
            os.write(DASH_DASH);

            mps.readBodyData(os);

            os.flush();
        }

        // print end boundary?

        // epilog
    }

    private void rewriteStream(InputStream is, PrintWriter w,
                               String ctxPath, String host)
        throws IOException, ParserException
    {
        // XXX charset from header
        Page page = new Page(is, null);
        Lexer lexer = new Lexer(page);
        Node n;
        RewriteVisitor v = new RewriteVisitor(w, ctxPath, host);
        while (null != (n = lexer.nextNode())) {
            n.accept(v);
        }

        w.flush();
    }

    private void rewriteStream(MultipartStream mps, PrintWriter w,
                               String ctxPath, String host)
        throws IOException
    {
        PipedInputStream pis = new PipedInputStream();
        Thread t = new Thread(new Rewriter(pis, w, ctxPath, host));
        PipedOutputStream pos = new PipedOutputStream(pis);
        t.start();
        int i = mps.readBodyData(pos);
        pos.close();
        try {
            t.join();
        } catch (InterruptedException exn) {
            logger.warn("interrupted while copying stream", exn);
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

    private class Rewriter implements Runnable
    {
        private final InputStream is;
        private final PrintWriter w;
        private final String ctxPath;
        private final String host;

        Rewriter(InputStream is, PrintWriter w, String ctxPath, String host)
        {
            this.is = is;
            this.w = w;
            this.ctxPath = ctxPath;
            this.host = host;
        }

        public void run()
        {
            try {
                rewriteStream(is, w, ctxPath, host);
            } catch (ParserException exn) {
                logger.warn("could not parse html", exn);
            } catch (IOException exn) {
                logger.warn("could not rewrite stream", exn);
            }
        }
    }
}
