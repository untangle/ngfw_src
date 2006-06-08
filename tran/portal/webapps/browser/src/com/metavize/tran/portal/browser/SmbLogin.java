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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metavize.mvvm.portal.PortalLogin;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class SmbLogin extends HttpServlet
{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
//         PortalLogin pl = (PortalLogin)req.getUserPrincipal();

//         String url = req.getParameter("url");
//         String d = req.getParameter("domain");
//         String u = req.getParameter("username");
//         String p = req.getParameter("password");

//         NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(d, u, p);
//         SmbFile f = new SmbFile(url, auth);
//         try {
//             f.listFiles();
//         } catch (SmbAuthException exn) {

//         } catch (SmbException exn) {

//         }
    }
}

