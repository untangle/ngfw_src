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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.log4j.Logger;

class UrlRewriter
{
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

    String getRemoteUrl()
        throws URIException
    {
        String s = new URI(remoteUrl).getEscapedURIReference();

        return s;
    }

    String getHost()
    {
        return host;
    }
}
