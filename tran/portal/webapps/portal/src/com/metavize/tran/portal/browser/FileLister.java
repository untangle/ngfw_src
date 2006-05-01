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
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;
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

    private static final String MIME_TYPES_PATH = "/etc/mime.types";

    private MimetypesFileTypeMap mimeMap;
    private Logger logger;

    // HttpServlet methods ----------------------------------------------------

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        HttpSession s = req.getSession();

        NtlmPasswordAuthentication auth = (NtlmPasswordAuthentication)s.getAttribute("ntlmPasswordAuthentication");
        if (null == auth) {
            auth = new NtlmPasswordAuthentication("windows.metavize.com", "amread", "XYZ123abc");
            s.setAttribute("ntlmPasswordAuthentication", auth);
        }

        resp.setContentType("text/xml");
        resp.addHeader("Cache-Control", "no-cache");

        String url = req.getParameter("url");
        String type = req.getParameter("type");

        SmbFileFilter filter = DIR_FILTER;
        if (null != type) {
            if (type.equalsIgnoreCase("full")) {
                filter = FULL_FILTER;
            } else if (type.equalsIgnoreCase("dir")) {
                filter = DIR_FILTER;
            } else {
                logger.warn("unknown listing type: " + type);
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
                logger.warn("not a directory: " + url);
                // XXX notify client
                return;
            }
        } catch (IOException exn) {
            throw new ServletException("could not emit listing", exn);
        } finally {
            if (null != os) {
                os.close();
            }
        }
    }

    @Override
    public void init() throws ServletException
    {
        logger = Logger.getLogger(getClass());

        try {
            mimeMap = new MimetypesFileTypeMap(MIME_TYPES_PATH);
        } catch (IOException exn) {
            logger.error("could not setup mimemap", exn);
            mimeMap = new MimetypesFileTypeMap();
        }
    }

    // private methods --------------------------------------------------------

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
                String contentType = f.isDirectory() ? ""
                    : mimeMap.getContentType(name);

                os.println("  <" + tag + " "
                           + "name='" + name + "' "
                           + "ctime='" + ctime + "' "
                           + "mtime='" + mtime + "' "
                           + "size='" + length + "' "
                           + "readable='" + readable + "' "
                           + "writable='" + writable + "' "
                           + "hidden='" + hidden + "' "
                           + "content-type='" + contentType + "'/>");
             }
        } catch (SmbException exn) {
            throw new ServletException("could not list directory", exn);
        }

        os.println("</root>");
    }
}
