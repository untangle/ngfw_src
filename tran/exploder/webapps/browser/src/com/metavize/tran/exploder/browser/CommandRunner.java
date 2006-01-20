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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jcifs.smb.NtlmPasswordAuthentication;
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

        System.out.println("PARAM: " + req.getParameter("command"));

        String[] files = req.getParameterValues("file");
        for (String f : files) {
            System.out.println("FILE: " + f);
        }
    }

    public void init() throws ServletException
    {
        logger = Logger.getLogger(getClass());
    }
}
