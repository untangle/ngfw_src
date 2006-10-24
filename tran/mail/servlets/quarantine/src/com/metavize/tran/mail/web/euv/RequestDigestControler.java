/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.mail.web.euv;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;

import com.metavize.tran.mail.papi.quarantine.QuarantineUserView;
import com.metavize.tran.mail.papi.quarantine.BadTokenException;
import com.metavize.tran.mail.papi.quarantine.QuarantineUserActionFailedException;
import com.metavize.tran.mail.papi.quarantine.NoSuchInboxException;

import com.metavize.tran.mime.EmailAddress;
import com.metavize.tran.mail.web.euv.tags.MessagesSetTag;

/**
 * Servlet controler for requesting new Digest emails.
 */
public class RequestDigestControler
  extends HttpServlet {

  
  protected void service(HttpServletRequest req,
    HttpServletResponse resp)
    throws ServletException, IOException {

    log("[service()] Called");

    //Check if they even entered a valid
    //email
    String reqAddr = req.getParameter(Constants.REQ_DIGEST_ADDR_RP);

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
    }
    catch(NoSuchInboxException ex) {
      MessagesSetTag.setErrorMessages(req,
        "Email address &#34;" + reqAddr + "&#34; does not have a quarantine inbox");
      req.getRequestDispatcher(Constants.REQ_DIGEST_VIEW).forward(req, resp);
      return;
    }
//    catch(QuarantineUserActionFailedException ex) {
//    }
    catch(Exception ex) {
      log("Exception servicing request", ex);
      req.getRequestDispatcher(Constants.SERVER_UNAVAILABLE_ERRO_VIEW).forward(req, resp);
      return;    
    }

  }
} 
