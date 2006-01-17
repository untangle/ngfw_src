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
                // XXX workgroups, servers?
                return f.isDirectory();
            }
        };

    private static final SmbFileFilter FULL_FILTER = new SmbFileFilter()
        {
            public boolean accept(SmbFile f)
                throws SmbException
            {
                // XXX non filesystem files?
                return true;
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

        SmbFileFilter filter = DIR_FILTER;
        ss = params.get("type");
        if (null != ss && 0 < ss.length) {
            String t = ss[0];
            if (t.equalsIgnoreCase("full")) {
                filter = FULL_FILTER;
            } else if (t.equalsIgnoreCase("dir")) {
                filter = DIR_FILTER;
            } else {
                logger.warn("unknown listing type: " + t);
            }
        }

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
                listDirectory(f, filter, os);
            } else {
                // XXX send back file data? from this servlet? in xml???
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

    private void listDirectory(SmbFile dir, SmbFileFilter filter,
                               PrintWriter os)
        throws IOException, ServletException
    {
        os.println("<?xml version=\"1.0\" ?>");

        os.println("<root path='" + dir.getPath() + "'>");

        try {
            for (SmbFile f : dir.listFiles(filter)) {
                String tag = f.isDirectory() ? "dir" : "file";
                String name = Util.escapeXml(f.getName());
                long ctime = f.createTime();
                long mtime = f.lastModified();
                long length = f.length();
                boolean readable = f.canRead();
                boolean writable = f.canWrite();
                boolean hidden = f.isHidden();

                os.println("  <" + tag + " "
                           + "name='" + name + "' "
                           + "ctime='" + ctime + "' "
                           + "mtime='" + mtime + "' "
                           + "size='" + length + "' "
                           + "readable='" + readable + "' "
                           + "writable='" + writable + "' "
                           + "hidden='" + hidden + "'/>");
             }
        } catch (SmbException exn) {
            throw new ServletException("could not list directory", exn);
        }

        os.println("</root>");
    }
}
