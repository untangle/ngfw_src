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

package com.untangle.tran.portal.browser;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Comparator;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.portal.LocalPortalManager;
import com.untangle.mvvm.portal.PortalLogin;
import com.untangle.mvvm.util.XmlUtil;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;
import jcifs.util.transport.TransportException;
import org.apache.log4j.Logger;

public class FileLister extends HttpServlet
{
    private static final SmbFileFilter DIR_FILTER = new SmbFileFilter()
        {
            public boolean accept(SmbFile f)
                throws SmbException
            {
                // XXX non filesystem files?
                boolean isDir;
                try {
                    isDir = f.isDirectory();
                } catch (NullPointerException exn) { // XXX bug in jcifs
                    isDir = false;
                }

                return isDir
                    && SmbFile.TYPE_NAMED_PIPE != f.getType()
                    && SmbFile.TYPE_PRINTER != f.getType()
                    && SmbFile.TYPE_COMM != f.getType();
            }
        };

    private static final SmbFileFilter FULL_FILTER = new SmbFileFilter()
        {
            public boolean accept(SmbFile f)
                throws SmbException
            {
                // XXX non filesystem files?
                return SmbFile.TYPE_NAMED_PIPE != f.getType()
                    && SmbFile.TYPE_PRINTER != f.getType()
                    && SmbFile.TYPE_COMM != f.getType();
            }
        };

    private static final String MIME_TYPES_PATH = "/etc/mime.types";

    private MimetypesFileTypeMap mimeMap;
    private LocalPortalManager portalManager;

    private Logger logger;

    // HttpServlet methods ----------------------------------------------------

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        try {
            runDoGet(req, resp);
        } catch (ServletException se) {
            Throwable throwable = se.getRootCause();
            /* XXX this is nasty, should be done with a proper try/catch */
            logger.debug("Caught an exception: ", se);

            /* XXX This is gnar gnar XXX */
            while (true) {
                if (throwable == null) break;

                logger.debug("Cause: ", throwable);

                /* XXX Should fix the SmbException to use the new
                 * initCause function in throwable */
                if (throwable instanceof SmbException) {
                    if (((SmbException)throwable).getRootCause() == null) {
                        break;
                    }
                    throwable = ((SmbException)throwable).getRootCause();
                } else {
                    if (throwable.getCause() == null) {
                        break;
                    }
                    throwable = throwable.getCause();
                }
            }

            if (throwable instanceof TransportException
                || throwable instanceof UnknownHostException) {
                // XXX send warning to user (listing may have already begun)
                logger.info("Error listing directory: ", throwable);
            } else {
                /* Rethrow the servlet exception */
                throw se;
            }
        }
    }

    protected void runDoGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        portalManager.incrementStatCounter(LocalPortalManager.CIFS_COUNTER);

        PortalLogin pl = (PortalLogin)req.getUserPrincipal();

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
            f = Util.getSmbFile(url, pl);
        } catch (MalformedURLException exn) {
            throw new ServletException(exn);
        }

        PrintWriter os = null;
        try {
            os = resp.getWriter();

            boolean isDir;
            try {
                isDir = f.isDirectory();
            } catch (NullPointerException exn) { // XXX bug in jcifs
                isDir = false;
            }

            if (isDir) {
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

        portalManager = MvvmContextFactory.context().portalManager();

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

        String p = dir.getPath().substring(4); // strip 'smb:'

        SmbFile[] files;
        try {
            files = dir.listFiles(filter);
        } catch (SmbAuthException exn) {
            os.println("<auth-error type='listFiles' url='" + p + "'/>");
            return;
        } catch (SmbException exn) {
            // XXX add error handling for client
            throw new ServletException("could not list directory", exn);
        }

        Arrays.sort(files, new Comparator<SmbFile>() {
            public int compare(SmbFile o1, SmbFile o2) {
                String o1Name = o1.getName();
                String o2Name = o2.getName();
                if (o1Name == null && o2Name == null) {
                    return 0;
                } if (o1Name == null) {
                    return -1;
                } if (o2Name == null) {
                    return 1;
                }
                return o1Name.compareTo(o2Name);
            }
        });

        os.println("<root path='" + p + "'>");

        try {
            for (SmbFile f : files) {
                boolean isDir;
                try {
                    isDir = f.isDirectory();
                } catch (NullPointerException exn) { // XXX bug in jcifs
                    isDir = false;
                }

                String tag = isDir ? "dir" : "file";
                String name = XmlUtil.escapeXml(f.getName());
                long ctime = f.createTime();
                long mtime = f.lastModified();
                long length = f.length();
                boolean readable = f.canRead();
                boolean writable = f.canWrite();
                boolean hidden = f.isHidden();
                String contentType = isDir ? ""
                    : mimeMap.getContentType(name);
                String principal = f.getPrincipal().toString();

                String type;
                switch (f.getType()) {
                case SmbFile.TYPE_FILESYSTEM:
                    if (isDir) {
                        type = "directory";
                    } else {
                        type = "file";
                    }
                    break;
                case SmbFile.TYPE_WORKGROUP:
                    type = "workgroup";
                    break;
                case SmbFile.TYPE_SERVER:
                    type = "server";
                    break;
                case SmbFile.TYPE_SHARE:
                    type = "share";
                    break;
                case SmbFile.TYPE_PRINTER:
                    type = "printer";
                    break;
                case SmbFile.TYPE_NAMED_PIPE:
                    type = "named_pipe";
                    break;
                case SmbFile.TYPE_COMM:
                    type = "comm";
                    break;
                default:
                    logger.warn("unknown type: " + f.getType());
                    type = "unknown";
                    break;
                }

                os.println("  <" + tag + " "
                           + "name='" + name + "' "
                           + "principal='" + principal + "' "
                           + "ctime='" + ctime + "' "
                           + "mtime='" + mtime + "' "
                           + "size='" + length + "' "
                           + "readable='" + readable + "' "
                           + "writable='" + writable + "' "
                           + "type='" + type + "' "
                           + "hidden='" + hidden + "' "
                           + "content-type='" + contentType + "'/>");
            }
        } catch (SmbException exn) {
            // XXX add error handling for client
            throw new ServletException("could not list directory", exn);
        }

        os.println("</root>");
    }
}
