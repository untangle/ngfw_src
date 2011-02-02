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

package com.untangle.uvm.servlet.alpaca;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.util.I18nUtil;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

/**
 * A reverse proxy for the alpaca.
 *
 * XXX in the future, put apache in front, and redirect from there.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class ProxyServlet extends HttpServlet
{
    private static final String BASE_URL;

    static {
        BASE_URL = "http://localhost:3000/alpaca";
    }

    private final HttpClientCache clientCache;
    private final Logger logger = Logger.getLogger(getClass());

    // constructors -----------------------------------------------------------

    public ProxyServlet()
    {
        clientCache = new HttpClientCache();
    }

    // HttpServlet methods ----------------------------------------------------

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        String url = getUrl(req);

        HttpMethod get = new GetMethod(url);
        get.setFollowRedirects(true);

        doIt(get, req, resp);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        String url = getUrl(req);
        RawPostMethod post = new RawPostMethod(url);
        //post.setFollowRedirects(true);
        try {
            post.setBodyStream(req.getContentType(), req.getInputStream(),
                               req.getIntHeader("Content-Length"));
            doIt(post, req, resp);
        } catch (IOException exn) {
            logger.warn("could not do post", exn);
        }
    }

    @SuppressWarnings("unused")
    private void doIt(HttpMethod method, HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        HttpSession s = req.getSession();

        HttpClient httpClient = clientCache.getClient(req, resp);
        HttpState state = httpClient.getState();

        InputStream is = null;
        OutputStream os = null;

        try {
            copyHeaders(req, method);
			int rc = httpClient.executeMethod(method);
            is = method.getResponseBodyAsStream();

            StatusLine sl = method.getStatusLine();
            resp.setStatus(sl.getStatusCode());
            copyHeaders(method, resp, req);

            for (Header h : method.getResponseHeaders()) {
                String name = h.getName();
                String value = h.getValue();

                if (name.equalsIgnoreCase("content-type")) {
                    resp.setContentType(value);

                    String v = value.toLowerCase();
                } else if (name.equalsIgnoreCase("date")) {
                    resp.setHeader(name, value);
                } else if (name.equalsIgnoreCase("etag")) {
                    resp.setHeader(name, value);
                } else if (name.equalsIgnoreCase("last-modified")) {
                    resp.setHeader(name, value);
                }
            }

            os = resp.getOutputStream();
            copyStream(is, os);

        } catch (UnknownHostException exn) {
            logger.warn("Unknown host (method: " + method +")", exn);
            try {
                LocalUvmContext uvm = LocalUvmContextFactory.context();
                Map<String,String> i18n_map = uvm.languageManager().getTranslations("untangle-libuvm");
                resp.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT, I18nUtil.tr("could not resolve host", i18n_map));
            } catch (IOException e) {
                logger.warn("could not send error page", e);
            }
        } catch (IOException exn) {
            logger.warn("IO Exception", exn);
            try {
                LocalUvmContext uvm = LocalUvmContextFactory.context();
                Map<String,String> i18n_map = uvm.languageManager().getTranslations("untangle-libuvm");
                resp.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT, I18nUtil.tr("request timed out", i18n_map));
            } catch (IOException e) {
                logger.warn("could not send error page", e);
            }
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
        String alpacaNonce = LocalUvmContextFactory.context()
            .adminManager().getAlpacaNonce();

        String pi = req.getPathInfo();
        String qs = req.getQueryString();
        if (qs == null || qs.equals("")) {
            qs = "argyle=" + alpacaNonce;
        } else {
            qs += "&argyle=" + alpacaNonce;
        }

        return BASE_URL + (null == pi ? "" : pi) + "?" + qs;
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

    @SuppressWarnings("unchecked") //getHeaderNames
    private void copyHeaders(HttpServletRequest req, HttpMethod method)
    {
        for (Enumeration<String> e = req.getHeaderNames(); e.hasMoreElements(); ) {
            String k = e.nextElement();
            if (k.equalsIgnoreCase("transfer-encoding")
                || k.equalsIgnoreCase("content-length")
                || k.equalsIgnoreCase("cookie")) {
                // skip
            } else if (k.equalsIgnoreCase("host")) {
                method.addRequestHeader("Host", "localhost");
            } else if (k.equalsIgnoreCase("referer")) {
                method.addRequestHeader("Referer", BASE_URL);
            } else {
                for (Enumeration<String> f = req.getHeaders(k); f.hasMoreElements(); ) {
                    String v = f.nextElement();
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
}
