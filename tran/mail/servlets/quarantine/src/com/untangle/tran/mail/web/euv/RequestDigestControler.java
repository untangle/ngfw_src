/*
 * Copyright (c) 2003-2006 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.tran.mail.web.euv;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import com.untangle.tran.mail.papi.quarantine.BadTokenException;
import com.untangle.tran.mail.papi.quarantine.NoSuchInboxException;
import com.untangle.tran.mail.papi.quarantine.QuarantineUserView;
import com.untangle.tran.mail.papi.quarantine.QuarantineUserActionFailedException;
import com.untangle.tran.mail.web.euv.tags.MessagesSetTag;
import com.untangle.tran.mime.EmailAddress;
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
