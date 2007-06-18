/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.mail.web.euv;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.node.mail.papi.quarantine.NoSuchInboxException;
import com.untangle.node.mail.papi.quarantine.QuarantineUserView;
import com.untangle.node.mail.web.euv.tags.MessagesSetTag;
import com.untangle.node.mime.EmailAddress;
import org.apache.log4j.Logger;

/**
 * Servlet controler for requesting new Digest emails.
 */
public class RequestDigestControler extends HttpServlet {

    private final Logger m_logger = Logger.getLogger(RequestDigestControler.class);

    protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        log("[service()] Called");

        //Check if they even entered a valid email
        String reqAddr = req.getParameter(Constants.REQ_DIGEST_ADDR_RP);
        //m_logger.info("email: " + reqAddr);
        if(reqAddr == null) {
            log("Digest request with null email");
            //Put a message in the request
            MessagesSetTag.setErrorMessages(req, "Please enter an email address");
            req.getRequestDispatcher(Constants.REQ_DIGEST_VIEW).forward(req, resp);
            return;
        }

        //Validate at least basic format
        if(EmailAddress.parseNE(reqAddr) == null) {
            log("Digest request with unparsable email");
            //Put a message in the request
            MessagesSetTag.setErrorMessages(req,
                                            "Please enter a valid email address (&#34;" + reqAddr + "&#34; is not valid)");
            req.getRequestDispatcher(Constants.REQ_DIGEST_VIEW).forward(req, resp);
            return;
        }

        //Get the QuarantineUserView reference.  If we cannot,
        //the user is SOL
        QuarantineUserView quarantine =
            QuarantineEnduserServlet.instance().getQuarantine();
        //m_logger.info("quarantine: " + quarantine);
        if(quarantine == null) {
            req.getRequestDispatcher(Constants.SERVER_UNAVAILABLE_ERRO_VIEW).forward(req, resp);
            return;
        }

        try {
            if(!quarantine.requestDigestEmail(reqAddr)) {
                log("Digest request returned false");
                //Put a message in the request
                MessagesSetTag.setErrorMessages(req,
                                                "Email address &#34;" + reqAddr + "&#34; does not have a quarantine inbox");
                req.getRequestDispatcher(Constants.REQ_DIGEST_VIEW).forward(req, resp);
                return;
            }

            log("Digest request for \"" + reqAddr +  "\" succeeded");
            //Put a message in the request
            MessagesSetTag.setInfoMessages(req,
                                           "Digest email sent to &#34;" + reqAddr + "&#34;.  Please wait for the email and follow instructions");
            req.getRequestDispatcher(Constants.REQ_DIGEST_VIEW).forward(req, resp);
            return;
        } catch(NoSuchInboxException ex) {
            //m_logger.info("no quarantine");
            MessagesSetTag.setErrorMessages(req,
                                            "Email address &#34;" + reqAddr + "&#34; does not have a quarantine inbox");
            req.getRequestDispatcher(Constants.REQ_DIGEST_VIEW).forward(req, resp);
            return;
        } catch(Exception ex) {
            //m_logger.info("general exception");
            log("Exception servicing request", ex);
            req.getRequestDispatcher(Constants.SERVER_UNAVAILABLE_ERRO_VIEW).forward(req, resp);
            return;
        }
    }
}
