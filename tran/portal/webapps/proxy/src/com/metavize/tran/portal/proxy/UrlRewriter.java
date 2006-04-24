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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.log4j.Logger;

class UrlRewriter
{
    private static final Pattern CSS_URL_PATTERN
        = Pattern.compile("url\\s*\\(\\s*(('[^']*')|(\"[^\"]*\"))\\s*\\)");

    private final String host;
    private final String contextBase;
    private final String localHost;
    private final String remoteUrl;

    private final Logger logger = Logger.getLogger(getClass());

    // constructors -----------------------------------------------------------

    UrlRewriter(HttpServletRequest req)
    {
        String p = req.getPathInfo();
        // XXX if p invalid, error page?
        int i = p.indexOf('/', 1);
        this.host = p.substring(1, 1 > i ? p.length() : i);
        this.contextBase = req.getContextPath();
        this.localHost = req.getServerName();

        String qs = req.getQueryString();
        this.remoteUrl = "http:/" + p + (null == qs ? "" : "?" + qs);
    }

    // package protected methods ----------------------------------------------

    String rewriteUrl(String v)
    {
        if (v.startsWith("http://")) {
            return "http://" + localHost + contextBase + v.substring(6);
        } else if (v.startsWith("//")) {
            return contextBase + "/" + v.substring(2);
        } else if (v.startsWith("/")) {
            return contextBase + "/" + host + v;
        } else {
            return v;
        }
    }

    String unwriteUrl(String v)
    {
        String absPrefix = "http://" + localHost + contextBase + "/" + host;
        if (v.startsWith(absPrefix)) {
            return "http://" + host + v.substring(absPrefix.length());
        } else {
            logger.warn("unexpected referer: " + v);
            return v;
        }
    }

    void filterCss(Reader r, Writer w)
        throws IOException
    {
        System.out.println("REWRITEING CSS");

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

    String getRemoteUrl()
        throws URIException
    {
        String s = new URI(remoteUrl, false).getEscapedURIReference();

        return s;
    }

    String getHost()
    {
        return host;
    }
}
