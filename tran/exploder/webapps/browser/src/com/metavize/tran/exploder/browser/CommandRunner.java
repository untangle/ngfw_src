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

        if (cmd.equals("delete")) {
            delete(req, auth);
        } else {
            throw new ServletException("bad command: " + cmd);
        }
    }

    public void init() throws ServletException
    {
        logger = Logger.getLogger(getClass());
    }

    private void delete(HttpServletRequest req,
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
}
