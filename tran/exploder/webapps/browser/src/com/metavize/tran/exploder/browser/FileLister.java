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
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;
import org.apache.catalina.util.RequestUtil;
import org.apache.log4j.Logger;

public class FileLister extends HttpServlet
{
    private static final SmbFileFilter DIR_FILTER = new SmbFileFilter()
        {
            public boolean accept(SmbFile f)
                throws SmbException
            {
                return f.isDirectory();
            }
        };

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
        resp.addHeader("Cache-Control", "no-cache");

        Map<String, String[]> params = new HashMap<String, String[]>();
        try {
            RequestUtil.parseParameters(params, req.getQueryString(), "UTF-8");
        } catch (UnsupportedEncodingException exn) {
            throw new ServletException("couldn't decode parameters", exn);
        }

        String[] ss = params.get("url");
        if (null == ss) {
            throw new ServletException("need url");
        }

        String url = ss[0];

        SmbFile f = null;

        try {
            f = new SmbFile(url, auth);
        } catch (MalformedURLException exn) {
            throw new ServletException(exn);
        }

        PrintWriter os = null;
        try {
            os = resp.getWriter();

            if (f.isDirectory()) {
                listDirectory(f, os);
            } else {
                // XXX
                os.println("A FILE");
            }
        } catch (IOException exn) {
            throw new ServletException("could not emit listing", exn);
        } finally {
            if (null != os) {
                os.close();
            }
        }
    }

    private void listDirectory(SmbFile f, PrintWriter os)
        throws IOException, ServletException
    {
        os.println("<?xml version=\"1.0\" ?>");

        os.println("<root path='" + f.getPath() + "'>");

        try {
            for (SmbFile d : f.listFiles(DIR_FILTER)) {
                os.println("  <dir name='" + Util.escapeXml(d.getName()) + "'/>");
             }
        } catch (SmbException exn) {
            throw new ServletException("could not list directory", exn);
        }

        os.println("</root>");
    }
}
