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

package com.metavize.tran.exploder.browser;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import org.apache.catalina.util.RequestUtil;
import org.apache.log4j.Logger;

public class FileLister extends HttpServlet
{
    private final Logger logger = Logger.getLogger(getClass());
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        HttpSession s = req.getSession();

        NtlmPasswordAuthentication auth = (NtlmPasswordAuthentication)s.getAttribute("ntlmPasswordAuthentication");
        if (null == auth) {
            auth = new NtlmPasswordAuthentication("metaloft.com", "dmorris", "chakas");
            s.setAttribute("ntlmPasswordAuthentication", auth);
        }

        resp.setContentType("text/xml");

        Map<String, String[]> params = new HashMap<String, String[]>();
        try {
            RequestUtil.parseParameters(params, req.getQueryString(), "UTF-8");
        } catch (UnsupportedEncodingException exn) {
            throw new ServletException("couldn't decode parameters", exn);
        }
        String[] ss = params.get("server");
        if (null == ss) {
            throw new ServletException("need server");
        }
        String server = ss[0];
        ss = params.get("path");
        String path = null == ss ? "/" : ss[0];

        String url = "smb://" + server + path;

        SmbFile f = null;

        try {
            f = new SmbFile(url, auth);
        } catch (MalformedURLException exn) {
            throw new ServletException(exn);
        }

        ServletOutputStream os = null;
        try {
            os = resp.getOutputStream();

            if (f.isDirectory()) {
                listDirectory(f, os);
            } else {
                os.println("A FILE");
            }
        } catch (IOException exn) {
            throw new ServletException("could not emit listing", exn);
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

    private void listDirectory(SmbFile f, ServletOutputStream os)
        throws IOException, ServletException
    {
        os.println("<?xml version=\"1.0\" ?>");
        os.println("<?xml-stylesheet type=\"text/xsl\" href=\"browser.xsl\"?>");

        // XXX do empty tree up to this point
        os.println("<dir name='" + f.getPath() + "'>");

        try {
            for (SmbFile d : f.listFiles()) {
                os.println("<dir name='" + escapeXml(d.getName()) + "'/>");
            }
        } catch (SmbException exn) {
            throw new ServletException("could not list directory", exn);
        }

        os.println("</dir>");
    }

    private String escapeXml(String in)
    {
        StringBuilder sb = new StringBuilder(in.length() + 32);
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
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

        return sb.toString();
    }
}
