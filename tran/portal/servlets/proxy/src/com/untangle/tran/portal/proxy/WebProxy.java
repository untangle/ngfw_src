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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmLocalContext;
import com.untangle.mvvm.NetworkManager;
import com.untangle.mvvm.portal.LocalPortalManager;
import com.untangle.mvvm.tran.IPaddr;
import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.fileupload.ParameterParser;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
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

    private static final String PERMISSIVE_COOKIE_POLICY = "PermissiveCookiePolicy";
    static {
        CookiePolicy.registerCookieSpec(PERMISSIVE_COOKIE_POLICY, PermissiveCookieSpec.class);
    }

    private static final Pattern CONTENT_TYPE_PATTERN
        = Pattern.compile("^Content-Type:\\s*(.*)$",
                          Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private MvvmLocalContext mvvmContext;
    private NetworkManager netManager;
    private LocalPortalManager portalManager;
    private Logger logger;

    @Override
    public void init() throws ServletException
    {
        mvvmContext = MvvmContextFactory.context();
        portalManager = mvvmContext.portalManager();
        netManager = mvvmContext.networkManager();
        Protocol p = new Protocol("https", new TrustingSslSocketFactory(), 443);
        Protocol.registerProtocol("https", p);
        logger = Logger.getLogger(getClass());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        try {
            UrlRewriter rewriter = UrlRewriter.getRewriter(req);
            String dest = rewriter.getHost();
            IPaddr addr = new IPaddr(InetAddress.getByName(dest));
            if (netManager.isAddressLocal(addr)) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN);
            } else {
                String remoteUrl = rewriter.getRemoteUrl();
                logger.info("GET remoteUrl: " + remoteUrl);
                HttpMethod method = new GetMethod(remoteUrl);
                doIt(req, resp, method, rewriter);
            }
        } catch (UnknownHostException exn) {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND);
        } catch (URIException exn) {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        try {
            UrlRewriter rewriter = UrlRewriter.getRewriter(req);
            String remoteUrl = rewriter.getRemoteUrl();
            logger.info("POST remoteUrl: " + remoteUrl);
            RawPostMethod method = new RawPostMethod(remoteUrl);

            method.setBodyStream(req.getContentType(), req.getInputStream(),
                                 req.getIntHeader("Content-Length"));
            doIt(req, resp, method, rewriter);
        } catch (URIException exn) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST);
        } catch (IOException exn) {
            logger.warn("could not process POST", exn);
        }
    }

    // private methods --------------------------------------------------------

    private void doIt(HttpServletRequest req, HttpServletResponse resp,
                      HttpMethod method, UrlRewriter rewriter)
        throws ServletException
    {
        method.getParams().setCookiePolicy(PERMISSIVE_COOKIE_POLICY);

        HttpSession s = req.getSession();

        portalManager.incrementStatCounter(LocalPortalManager.PROXY_COUNTER);

        method.setFollowRedirects(false);
        HttpMethodParams params = method.getParams();

        HttpClient httpClient = (HttpClient)s.getAttribute(HTTP_CLIENT);
        if (null == httpClient) {
            httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
            s.setAttribute(HTTP_CLIENT, httpClient);
        }

        copyHeaders(req, method, rewriter);

        try {
            int rc = httpClient.executeMethod(method); // XXX release method
            resp.setStatus(rc);
            copyHeaders(method, resp, rewriter);
            InputStream is = method.getResponseBodyAsStream();
            if (null != is) {
                processResponse(is, resp, rewriter);
            }
        } catch (IOException exn) {
            logger.warn("could not make request", exn);
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST);
            return;
        } finally {
            method.releaseConnection();
        }
    }

    private void sendError(HttpServletResponse resp, int code)
        throws ServletException
    {
        try {
            resp.sendError(code);
        } catch (IOException exn) {
            throw new ServletException("could not send error", exn);
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
                method.addRequestHeader("Referer", rewriter.unwriteUrl(v));
             } else if (k.equalsIgnoreCase("user-agent")) {
                method.addRequestHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8) Gecko/20051111 ]Firefox/1.5");
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
                resp.setHeader(name, rewriter.rewriteUrl(value));
            } else if (name.equalsIgnoreCase("set-cookie")) {
                // don't forward
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
            try {
                PrintWriter w = new PrintWriter(resp.getWriter());
                rewriteStream(is, w, rewriter);
            } catch (ParserException exn) {
                logger.warn("could not parse html", exn);
            }
        } else if (contentType.startsWith("multipart/")) {
            OutputStream os = resp.getOutputStream();
            handleMultipart(contentType, is, os, rewriter);
        } else if (contentType.startsWith("text/css")) {
            Reader r = new InputStreamReader(is);
            Writer w = resp.getWriter();
            rewriter.filterCss(r, w);
        } else if (contentType.startsWith("text/javascript")
                   || contentType.startsWith("application/x-javascript")) {
            Reader r = new InputStreamReader(is);
            Writer w = resp.getWriter();
            rewriter.filterJavaScript(r, w);
        } else {
            OutputStream os = resp.getOutputStream();
            copyStream(is, os);
        }
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
        Thread t = mvvmContext.newThread(new RewriteWorker(pis, w, rewriter));
        PipedOutputStream pos = new PipedOutputStream(pis);
        t.start();
        try {
            int i = mps.readBodyData(pos);
        } finally {
            pos.close();
        }
        try {
            t.join();
        } catch (InterruptedException exn) {
            logger.warn("interrupted while copying stream", exn);
        }
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
