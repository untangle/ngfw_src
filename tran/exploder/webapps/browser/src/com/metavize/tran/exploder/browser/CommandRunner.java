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

import java.net.MalformedURLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jcifs.smb.NtlmPasswordAuthentication;
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
        HttpSession s = req.getSession();

        NtlmPasswordAuthentication auth = (NtlmPasswordAuthentication)s.getAttribute("ntlmPasswordAuthentication");
        if (null == auth) {
            auth = new NtlmPasswordAuthentication("bebe", "kaka", "poopoo");
            s.setAttribute("ntlmPasswordAuthentication", auth);
        }

        String cmd = req.getParameter("command");

        if (cmd.equals("rm")) {
            rm(req, auth);
        } else if (cmd.equals("mv")) {
            mv(req, auth);
        } else if (cmd.equals("cp")) {
            cp(req, auth);
        } else {
            throw new ServletException("bad command: " + cmd);
        }
    }

    public void init() throws ServletException
    {
        logger = Logger.getLogger(getClass());
    }

    private void rm(HttpServletRequest req,
                    NtlmPasswordAuthentication auth)
    {
        String[] files = req.getParameterValues("file");

        for (String f : files) {
            try {
                new SmbFile(f, auth).delete();
            } catch (SmbException exn) {
                logger.warn("could not delete: " + f, exn);
            } catch (MalformedURLException exn) {
                logger.warn("bad url: " + f, exn);
            }
        }
    }

    private void mv(HttpServletRequest req, NtlmPasswordAuthentication auth)
    {
        String[] s = req.getParameterValues("src");
        String d = req.getParameter("dest");

        for (String f : s) {
            try {
                SmbFile src = new SmbFile(f, auth);
                SmbFile dest = new SmbFile(d + src.getName(), auth);
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

    private void cp(HttpServletRequest req, NtlmPasswordAuthentication auth)
    {
        String[] s = req.getParameterValues("src");
        String d = req.getParameter("dest");

        for (String f : s) {
            try {
                SmbFile src = new SmbFile(f, auth);
                SmbFile dest = new SmbFile(d + src.getName(), auth);
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
}
