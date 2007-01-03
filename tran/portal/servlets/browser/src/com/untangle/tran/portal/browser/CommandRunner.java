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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.mvvm.portal.PortalLogin;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import org.apache.log4j.Logger;

public class CommandRunner extends HttpServlet
{
    private static final String MIME_TYPES_PATH = "/etc/mime.types";

    private Logger logger;

    // HttpServlet methods ----------------------------------------------------

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        PortalLogin pl = (PortalLogin)req.getUserPrincipal();

        resp.setContentType("text/xml");
        resp.addHeader("Cache-Control", "no-cache");

        String cmd = req.getParameter("command");

        if (cmd.equals("rm")) {
            rm(req, resp, pl);
        } else if (cmd.equals("mv")) {
            mv(req, resp, pl);
        } else if (cmd.equals("cp")) {
            cp(req, resp, pl);
        } else if (cmd.equals("mkdir")) {
            mkdir(req, resp, pl);
        } else if (cmd.equals("rename")) {
            rename(req, resp, pl);
        } else {
            throw new ServletException("bad command: " + cmd);
        }
    }

    public void init() throws ServletException
    {
        logger = Logger.getLogger(getClass());
    }

    private void rm(HttpServletRequest req, HttpServletResponse resp,
                    PortalLogin pl)
        throws ServletException
    {
        String[] files = req.getParameterValues("file");

        for (String f : files) {
            try {
                Util.getSmbFile(f, pl).delete();
            } catch (SmbAuthException exn) {
                sendError(resp, "Not authorized to remove files.");
            } catch (SmbException exn) {
                sendError(resp, "Could not remove files");
            } catch (MalformedURLException exn) {
                logger.warn("bad url: " + f, exn);
            }
        }
    }

    private void mv(HttpServletRequest req, HttpServletResponse resp,
                    PortalLogin pl)
        throws ServletException
    {
        String[] s = req.getParameterValues("src");
        String d = req.getParameter("dest");

        for (String f : s) {
            try {
                SmbFile src = Util.getSmbFile(f, pl);
                SmbFile dest = Util.getSmbFile(d + src.getName(), pl);
                src.renameTo(dest);
                sendSuccess(resp);
            } catch (SmbAuthException exn) {
                sendError(resp, "Not authorized to move files.");
            } catch (SmbException exn) {
                sendError(resp, "Could not move files.");
            } catch (MalformedURLException exn) {
                // XXX report errors to client
                logger.warn("bad url", exn);
            }
        }
    }

    private void rename(HttpServletRequest req, HttpServletResponse resp,
                        PortalLogin pl)
        throws ServletException
    {
        String src = req.getParameter("src");
        String dest = req.getParameter("dest");

        try {
            SmbFile destFile = Util.getSmbFile(dest, pl);
            Util.getSmbFile(src, pl).renameTo(destFile);
            sendSuccess(resp);
        } catch (SmbAuthException exn) {
            sendError(resp, "Not authorized to rename files.");
        } catch (SmbException exn) {
            sendError(resp, "Could not rename files.");
        } catch (MalformedURLException exn) {
            logger.warn("bad url: " + src + " or: " + dest, exn);
        }
    }

    private void cp(HttpServletRequest req, HttpServletResponse resp,
                    PortalLogin pl)
        throws ServletException
    {
        String[] s = req.getParameterValues("src");
        String d = req.getParameter("dest");

        for (String f : s) {
            try {
                SmbFile src = Util.getSmbFile(f, pl);
                SmbFile dest = Util.getSmbFile(d + src.getName(), pl);
                src.copyTo(dest);
                sendSuccess(resp);
            } catch (SmbAuthException exn) {
                sendError(resp, "Not authorized to copy files.");
            } catch (SmbException exn) {
                sendError(resp, "Could not copy files.");
            } catch (MalformedURLException exn) {
                // XXX report errors to client
                logger.warn("bad url", exn);
            }
        }
    }

    private void mkdir(HttpServletRequest req, HttpServletResponse resp,
                       PortalLogin pl)
        throws ServletException
    {
        String url = req.getParameter("url");

        try {
            Util.getSmbFile(url, pl).mkdir();
            sendSuccess(resp);
        } catch (SmbAuthException exn) {
            sendError(resp, "Not authorized to make directory.");
        } catch (SmbException exn) {
            sendError(resp, "Could not make directory.");
        } catch (MalformedURLException exn) {
            // XXX
            logger.warn("bad url:" + url, exn);
        }
    }

    private void sendError(HttpServletResponse resp, String s)
        throws ServletException
    {
        try {
            PrintWriter os = resp.getWriter();
            os.println("<auth-error msg='" + s + "'/>");
        } catch (IOException exn) {
            throw new ServletException("could not send error", exn);
        }
    }

    private void sendSuccess(HttpServletResponse resp)
        throws ServletException
    {
        try {
            PrintWriter os = resp.getWriter();
            os.println("<success/>");
        } catch (IOException exn) {
            throw new ServletException("could not send error", exn);
        }
    }
}
