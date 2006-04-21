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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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

        UrlRewriter rewriter = new UrlRewriter(req);

        copyHeaders(req, method, rewriter);

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

        copyHeaders(method, resp, rewriter);

        InputStream is = null;
        try {
            is = method.getResponseBodyAsStream();
            processResponse(is, resp, rewriter);
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

    private void copyHeaders(HttpServletRequest req, HttpMethod method,
                             UrlRewriter rewriter)
    {
        for (Enumeration e = req.getHeaderNames(); e.hasMoreElements(); ) {
            String k = (String)e.nextElement();
            if (k.equalsIgnoreCase("transfer-encoding")
                || k.equalsIgnoreCase("content-length")
                || k.equalsIgnoreCase("cookie")) {
                // skip
            } else if (k.equalsIgnoreCase("host")) {
                method.addRequestHeader("Host", rewriter.getHost());
            } else if (k.equalsIgnoreCase("referer")) {
                String v = req.getHeader(k);
                System.out.println("UNWRITE: " + rewriter.unwriteUrl(v));
                method.addRequestHeader("Referer", rewriter.unwriteUrl(v));
            } else {
                for (Enumeration f = req.getHeaders(k); f.hasMoreElements(); ) {
                    String v = (String)f.nextElement();
                    method.addRequestHeader(k, v);
                }
            }
        }
    }

    private void copyHeaders(HttpMethod method, HttpServletResponse resp,
                             UrlRewriter rewriter)
    {
        for (Header h : method.getResponseHeaders()) {
            String name = h.getName();
            String value = h.getValue();

            if (name.equalsIgnoreCase("content-type")) {
                resp.setContentType(value);
            } else if (name.equalsIgnoreCase("transfer-encoding")
                       || name.equalsIgnoreCase("content-length")) {
                // don't forward
            } else if (name.equalsIgnoreCase("location")
                       || name.equalsIgnoreCase("content-location")) {
                System.out.println("SET RESP HEADER: " + name + ": "
                                   + rewriter.rewriteUrl(value));
                resp.setHeader(name, rewriter.rewriteUrl(value));
            } else {
                resp.setHeader(name, value);
            }
        }
    }

    private void processResponse(InputStream is, HttpServletResponse resp,
                                 UrlRewriter rewriter)
        throws IOException
    {
        String contentType = resp.getContentType();
        contentType = null == contentType ? "" : contentType.toLowerCase();

        if (contentType.startsWith("text/html")) {
            PrintWriter w = null;
            try {
                w = new PrintWriter(resp.getWriter());
                rewriteStream(is, w, rewriter);
            } catch (ParserException exn) {
                logger.warn("could not parse html", exn);
            } finally {
                if (null != w) {
                    w.close();
                }
            }
        } else if (contentType.startsWith("multipart/")) {
            OutputStream os = null;
            try {
                os = resp.getOutputStream();
                handleMultipart(contentType, is, os, rewriter);
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
    }

    private String getUrl(HttpServletRequest req)
    {
        String p = req.getPathInfo();
        String qs = req.getQueryString();

        return "http:/" + p + (null == qs ? "" : "?" + qs);
    }

    private void handleMultipart(String contentType,
                                 InputStream is, OutputStream os,
                                 UrlRewriter rewriter)
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
                        rewriteStream(mps, w, rewriter);
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
    }

    private void rewriteStream(InputStream is, PrintWriter w,
                               UrlRewriter rewriter)
        throws IOException, ParserException
    {
        // XXX charset from header
        Page page = new Page(is, null);
        Lexer lexer = new Lexer(page);
        Node n;
        RewriteVisitor v = new RewriteVisitor(w, rewriter);
        while (null != (n = lexer.nextNode())) {
            n.accept(v);
        }

        w.flush();
    }

    private void rewriteStream(MultipartStream mps, PrintWriter w,
                               UrlRewriter rewriter)
        throws IOException
    {
        PipedInputStream pis = new PipedInputStream();
        Thread t = new Thread(new RewriteWorker(pis, w, rewriter));
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

    private class RewriteWorker implements Runnable
    {
        private final InputStream is;
        private final PrintWriter w;
        private final UrlRewriter rewriter;

        RewriteWorker(InputStream is, PrintWriter w, UrlRewriter rewriter)
        {
            this.is = is;
            this.w = w;
            this.rewriter = rewriter;
        }

        public void run()
        {
            try {
                rewriteStream(is, w, rewriter);
            } catch (ParserException exn) {
                logger.warn("could not parse html", exn);
            } catch (IOException exn) {
                logger.warn("could not rewrite stream", exn);
            }
        }
    }
}
