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

import java.net.MalformedURLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metavize.mvvm.portal.PortalLogin;
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

        String cmd = req.getParameter("command");

        try {
            if (cmd.equals("rm")) {
                rm(req, pl);
            } else if (cmd.equals("mv")) {
                mv(req, pl);
            } else if (cmd.equals("cp")) {
                cp(req, pl);
            } else if (cmd.equals("mkdir")) {
                mkdir(req, pl);
            } else if (cmd.equals("rename")) {
                rename(req, pl);
            } else {
                throw new ServletException("bad command: " + cmd);
            }
        } catch (AuthenticationException exn) {
            Util.sendAuthenicationError(resp, exn);
        }
    }

    public void init() throws ServletException
    {
        logger = Logger.getLogger(getClass());
    }

    private void rm(HttpServletRequest req, PortalLogin pl)
        throws AuthenticationException
    {
        String[] files = req.getParameterValues("file");

        for (String f : files) {
            try {
                Util.authenticateFile("smb:" + f, pl).delete();
            } catch (SmbException exn) {
                logger.warn("could not delete: " + f, exn);
            } catch (MalformedURLException exn) {
                logger.warn("bad url: " + f, exn);
            }
        }
    }

    private void mv(HttpServletRequest req, PortalLogin pl)
        throws AuthenticationException
    {
        String[] s = req.getParameterValues("src");
        String d = "smb:" + req.getParameter("dest");

        for (String f : s) {
            try {
                SmbFile src = Util.authenticateFile("smb:" + f, pl);
                SmbFile dest = Util.authenticateFile(d + src.getName(), pl);
                src.renameTo(dest);
            } catch (SmbException exn) {
                // XXX report errors to client
                logger.warn("could not move: " + f, exn);
            } catch (MalformedURLException exn) {
                // XXX report errors to client
                logger.warn("bad url", exn);
            }
        }
    }

    private void rename(HttpServletRequest req, PortalLogin pl)
        throws AuthenticationException
    {
        String src = "smb:" + req.getParameter("src");
        String dest = "smb:" + req.getParameter("dest");

        try {
            SmbFile destFile = Util.authenticateFile(dest, pl);
            Util.authenticateFile(src, pl).renameTo(destFile);
        } catch (SmbException exn) {
            logger.warn("could not rename: " + src + " to: " + dest, exn);
        } catch (MalformedURLException exn) {
            logger.warn("bad url: " + src + " or: " + dest, exn);
        }
    }

    private void cp(HttpServletRequest req, PortalLogin pl)
        throws AuthenticationException
    {
        String[] s = req.getParameterValues("src");
        String d = "smb:" + req.getParameter("dest");

        for (String f : s) {
            try {
                SmbFile src = Util.authenticateFile("smb:" + f, pl);
                SmbFile dest = Util.authenticateFile(d + src.getName(), pl);
                src.copyTo(dest);
            } catch (SmbException exn) {
                // XXX report errors to client
                logger.warn("could not move: " + f, exn);
            } catch (MalformedURLException exn) {
                // XXX report errors to client
                logger.warn("bad url", exn);
            }
        }
    }

    private void mkdir(HttpServletRequest req, PortalLogin pl)
        throws AuthenticationException
    {
        String url = "smb:" + req.getParameter("url");

        try {
            Util.authenticateFile(url, pl).mkdir();
        } catch (MalformedURLException exn) {
            // XXX
            logger.warn("bad url:" + url, exn);
        } catch (SmbException exn) {
            logger.warn("could not make directory" + url, exn);
        }
    }
}
