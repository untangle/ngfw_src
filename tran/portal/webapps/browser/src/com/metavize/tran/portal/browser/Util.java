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

package com.metavize.tran.portal.browser;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.metavize.mvvm.portal.PortalLogin;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

class Util
{
    static SmbFile authenticateFile(String url, PortalLogin pl)
        throws AuthenticationException, MalformedURLException
    {
        for (NtlmPasswordAuthentication npa : (List<NtlmPasswordAuthentication>)pl.getAuthenticators()) {
            try {
                SmbFile f = new SmbFile(url, npa);
                f.exists();
                return f;
            } catch (SmbException exn) { }
        }

        throw new AuthenticationException(url);
    }

    static void sendAuthenicationError(HttpServletResponse resp, AuthenticationException exn)
        throws ServletException
    {
        String url = exn.getUrl();
        if (url.startsWith("smb:")) {
            url = url.substring(3);
        }

        try {
            PrintWriter w = resp.getWriter();
            w.println("<error type='auth' url='" + escapeXml(url) + "'/>");
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    static String escapeXml(String in)
    {
        StringBuilder sb = null;
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (null == sb) {
                switch (c) {
                case '<': case '>': case '\'': case '&': case '"':
                    sb = new StringBuilder(in.length() + 32);
                    sb.append(in, 0, i);
                    break;
                }
            }

            if (null != sb) {
                switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                default:
                    sb.append(c);
                    break;
                }
            }
        }

        return null != sb ? sb.toString() : in;
    }

    static String stripSlash(String s)
    {
        while ('/' == s.charAt(s.length() - 1)) {
            s = s.substring(0, s.length() - 1);
        }

        return s;
    }
}
