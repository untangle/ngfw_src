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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.URIException;
import org.apache.log4j.Logger;

class UrlRewriter
{
    private static final String SLASH = "/";
    private static final String SLASH_SLASH = "//";
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final String JAVASCRIPT = "javascript:";

    private static final String JAVASCRIPT_REPLACEMENTS = "jsrepl.sed";

    private static final Pattern CSS_URL_PATTERN
        = Pattern.compile("url\\s*\\(\\s*(('[^']*')|(\"[^\"]*\"))\\s*\\)",
                          Pattern.CASE_INSENSITIVE);

    private final String localHost;
    private final String contextBase;
    private final String proto;
    private final String contextBaseProto;
    private final String host;
    private final String contextBaseProtoHost;
    private final String remoteUrl;

    private final Logger logger = Logger.getLogger(getClass());

    // constructors -----------------------------------------------------------

    private UrlRewriter(String localHost, String contextBase,
                        String proto, String host, String remoteUrl)
    {
        this.localHost = localHost;
        this.contextBase = contextBase;
        this.proto = proto;
        this.contextBaseProto = contextBase + proto + "/";
        this.host = host;
        this.contextBaseProtoHost = contextBaseProto + host + "/";
        this.remoteUrl = remoteUrl;
    }

    // factories --------------------------------------------------------------

    static UrlRewriter getRewriter(HttpServletRequest req)
        throws ServletException
    {
        String localHost = req.getServerName();

        String reqUri = req.getRequestURI();
        int i = reqUri.indexOf("/http");
        if (0 > i) {
            throw new ServletException("bad uri: " + reqUri);
        }
        String contextBase = reqUri.substring(0, ++i);
        int j = i + "http".length();
        if (reqUri.length() <= j) {
            throw new ServletException("bad uri: " + reqUri);
        }
        char c = reqUri.charAt(j);

        String proto;
        if ('/' == c) {
            proto = "http";
            i = j + 1;
        } else if ('s' == c) {
            if (reqUri.length() <= ++j) {
                throw new ServletException("bad uri: " + reqUri);
            }
            proto = "https";
            i = j + 1;
        } else {
            throw new ServletException("bad proto in uri: " + reqUri);
        }

        String host;
        String uri;
        j = reqUri.indexOf("/", i);
        if (0 > j) {
            Logger.getLogger(UrlRewriter.class).warn("strange request uri: " + reqUri);
            host = reqUri.substring(i);
            uri = "/";
        } else {
            host = reqUri.substring(i, j);
            uri = reqUri.substring(j);
        }

        String queryString = req.getQueryString();
        String url = proto + "://" + host + uri
            + (null == queryString ? "" : "?" + queryString);

        return new UrlRewriter(localHost, contextBase, proto, host, url);
    }

    // package protected methods ----------------------------------------------

    String rewriteUrl(String v)
    {
        if (v.startsWith(HTTP)) {
            return HTTPS + localHost + contextBase + "http/"
                + v.substring(HTTP.length()); // XXX https
        } else if (v.startsWith(HTTPS)) {
            return HTTPS + localHost + contextBase + "https/"
                + v.substring(HTTPS.length()); // XXX https
        } else if (v.startsWith(SLASH_SLASH)) {
            return SLASH_SLASH + localHost + contextBaseProto
                + v.substring(SLASH_SLASH.length());
        } else if (v.startsWith(SLASH)) {
            return contextBaseProtoHost + v.substring(SLASH.length());
        } else if (v.startsWith(JAVASCRIPT)) {
            return JAVASCRIPT + rewriteJavaScript(v.substring(JAVASCRIPT.length()));
        } else {
            return v;
        }
    }

    String unwriteUrl(String v)
    {
        // XXX HTTPS
        String absPrefix = HTTP + localHost + contextBaseProtoHost;
        if (v.startsWith(absPrefix)) {
            return "http://" + host + v.substring(absPrefix.length() - 1);
        } else {
            absPrefix = HTTPS + localHost + contextBaseProtoHost;
            if (v.startsWith(absPrefix)) {
                return "https://" + host + v.substring(absPrefix.length() - 1);
            } else {
                return v;
            }
        }
    }

    void writeJavaScript(PrintWriter w)
    {
        w.println("\n<script type='text/javascript'>");

        w.print("var mv_localHost = '");
        w.print(localHost);
        w.println("';");
        w.print("var mv_contextBase = '");
        w.print(contextBase);
        w.println("';");
        w.print("var mv_proto = '");
        w.print(proto);
        w.println("';");
        w.print("var mv_host = '");
        w.print(host);
        w.println("';");

        w.println("</script>");
        w.print("<script type='text/javascript' src='");
        w.print(contextBase);
        w.println("mvrepl.js'></script>");
    }

    void filterCss(Reader r, Writer w)
        throws IOException
    {
        BufferedReader br = new BufferedReader(r);

        CharSequence l;
        while (null != (l = br.readLine())) {
            Matcher m = CSS_URL_PATTERN.matcher(l);

            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String rep = m.group(1);
                rep = rewriteUrl(rep.substring(1, rep.length() - 1));
                m.appendReplacement(sb, "url('" + rep + "')");
            }
            m.appendTail(sb);
            l = sb;

            w.append(l);
            w.append("\n");
        }
    }

    void filterJavaScript(Reader r, Writer w)
        throws IOException
    {
        InputStream is = getClass().getClassLoader()
            .getResourceAsStream(JAVASCRIPT_REPLACEMENTS);

        List<Replacement> repls;
        if (null == is) {
            repls = Collections.emptyList();
        } else {
            repls = Replacement.getReplacements(is);
        }

        filterReplace(r, w, repls);
    }

    String rewriteJavaScript(String text)
    {
        Reader r = new StringReader(text);
        Writer w = new StringWriter(text.length());
        try {
            filterJavaScript(r, w);
        } catch (IOException exn) {
            logger.error("this isn't happening!", exn);
        }

        return w.toString();
    }

    String getRemoteUrl()
        throws URIException
    {
        return remoteUrl;
    }

    String getHost()
    {
        return host;
    }

    // private methods --------------------------------------------------------

    private void filterReplace(Reader r, Writer w, List<Replacement> repls)
        throws IOException
    {
        BufferedReader br = new BufferedReader(r);

        logger.debug("Filtering JavaScript");

        String l;
        while (null != (l = br.readLine())) {
            for (Replacement repl : repls) {
                l = repl.doReplacement(l);
            }

            w.append(l).append("\n");
            logger.debug(l);
        }

        logger.debug("Filtered JavaScript");
    }
}
