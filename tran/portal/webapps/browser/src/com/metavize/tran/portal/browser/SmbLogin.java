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

import java.io.IOException;
import java.net.UnknownHostException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metavize.mvvm.portal.PortalLogin;
import jcifs.UniAddress;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbSession;

public class SmbLogin extends HttpServlet
{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        PortalLogin pl = (PortalLogin)req.getUserPrincipal();

        String d = req.getParameter("domain");
        String u = req.getParameter("username");
        String p = req.getParameter("password");

        NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(d, u, p);
        try {
            try {
                UniAddress ua = UniAddress.getByName(d);
                SmbSession.logon(ua, auth);
                System.out.println("AUTHENTICATED!!!");
            } catch (UnknownHostException exn) {
                System.out.println("UNKNOWN HOST!!!");
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch(SmbAuthException sae) {
                System.out.println("AUTHENTICATED!!!");
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch(SmbException se) {
                System.out.println("WTF!!!");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (IOException exn) {
            throw new ServletException(exn);
        }
    }
}

