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
import java.net.MalformedURLException;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import org.apache.log4j.Logger;

public class FileGetter extends HttpServlet
{
    private static final String MIME_TYPES_PATH = "/etc/mime.types";

    // XXX attach map to container context
    private MimetypesFileTypeMap mimeMap;

    private Logger logger;

    // HttpServlet methods ----------------------------------------------------

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        HttpSession s = req.getSession();

        // XXX auth
        NtlmPasswordAuthentication auth = (NtlmPasswordAuthentication)s.getAttribute("ntlmPasswordAuthentication");
        if (null == auth) {
            auth = new NtlmPasswordAuthentication("metaloft.com", "dmorris", "chakas");
            s.setAttribute("ntlmPasswordAuthentication", auth);
        }

        //SmbFile f = null;

//         try {
//             f = new SmbFile(url, auth);
//         } catch (MalformedURLException exn) {
//             throw new ServletException(exn);
//         }

        PrintWriter os = null;
        try {
            os = resp.getWriter();
            System.out.println("PATH: " + req.getPathInfo());
            os.println("PATH: " + req.getPathInfo());

//             if (f.isDirectory()) {
//                 // XXX
//                 throw new ServletException("not a file: " + f);
//             } else {
//                 resp.setContentType("text/plain"); // XXX
//                 resp.addHeader("Cache-Control", "no-cache");
//                 os.println(f + " CONTENT-TYPE: " + mimeMap.getContentType(f.getName()));
//             }
        } catch (IOException exn) {
            throw new ServletException("could not emit listing", exn);
        } finally {
            if (null != os) {
                os.close();
            }
        }
    }

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
}
