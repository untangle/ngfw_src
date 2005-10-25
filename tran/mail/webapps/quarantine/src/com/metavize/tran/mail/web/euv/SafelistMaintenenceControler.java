/*
 * Copyright (c) 2005 Metavize Inc.
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

import com.metavize.tran.mail.web.euv.tags.MessagesSetTag;
import com.metavize.tran.mail.web.euv.tags.CurrentEmailAddressTag;
import com.metavize.tran.mail.web.euv.tags.CurrentAuthTokenTag;

import com.metavize.tran.mail.papi.quarantine.QuarantineUserView;
import com.metavize.tran.mail.papi.quarantine.BadTokenException;
import com.metavize.tran.mail.papi.safelist.SafelistEndUserView;
import com.metavize.tran.mail.papi.safelist.NoSuchSafelistException;

import com.metavize.tran.mail.web.euv.tags.HasSafelistTag;
import com.metavize.tran.mail.web.euv.tags.SafelistListTag;
import com.metavize.tran.mail.web.euv.tags.SafelistEntryTag;

import sun.misc.BASE64Decoder;


/**
 * Controler used for Safelist self-service maintenence
 */
public class SafelistMaintenenceControler
  extends HttpServlet {

  protected void service(HttpServletRequest req,
    HttpServletResponse resp)
    throws ServletException, IOException {

    String authTkn = req.getParameter(Constants.AUTH_TOKEN_RP);
    if(authTkn == null) {
      log("[SafelistMaintenenceControler] Auth token null");
      req.getRequestDispatcher(Constants.REQ_DIGEST_VIEW).forward(req, resp);
      return;
    }

    //Get the QuarantineUserView reference.  If we cannot,
    //the user is SOL
    SafelistEndUserView safelist =
      QuarantineEnduserServlet.instance().getSafelist();
    QuarantineUserView quarantine =
      QuarantineEnduserServlet.instance().getQuarantine();
    if(safelist == null) {
      log("[SafelistMaintenenceControler] Safelist Hosed");
      req.getRequestDispatcher(Constants.SERVER_UNAVAILABLE_ERRO_VIEW).forward(req, resp);
      return;
    }


    //Now, we wrap a bunch of calls in the same try/catch
    //block as the outcome based on exception type
    //is the same
    try {
      //Attempt to decrypt their token
      String account = quarantine.getAccountFromToken(authTkn);

      CurrentEmailAddressTag.setCurrent(req, account);
      CurrentAuthTokenTag.setCurrent(req, authTkn);

      //Now, figure out what they wanted to do.  Either
      //sladd, slremove, or simply to see the safelist
      String action = req.getParameter(Constants.ACTION_RP);
      log("[SafelistMaintenenceControler] Action " + action);
      if(action == null) {
        action = Constants.SAFELIST_VIEW_RV;
      }

      //A "result" message, which may be displayed if
      //they requested an action
      String msg = null;

      //No matter what action we take, the outcome
      //is a summary of the safelist
      String[] safelistContents = null;
      String[] targetAddresses = req.getParameterValues(Constants.SAFELIST_TARGET_ADDR_RP);
//      String targetAddress = base64DecodeAddress(req.getParameter(Constants.SAFELIST_TARGET_ADDR_RP));

      if(action.equals(Constants.SAFELIST_ADD_RV) &&
        targetAddresses != null) {
        
        log("[SafelistMaintenenceControler] Add request " +
          targetAddresses.length + " addresses for account \"" + account + "\"");
        for(String addr : targetAddresses) {
          safelistContents = safelist.addToSafelist(account, addr);
        }
        msg = targetAddresses.length + " addresses added to safelist";
        //TODO bscott Go through the inbox, releasing any messages w/ this newly
        //     added address.
      }
      else if(action.equals(Constants.SAFELIST_REMOVE_RV) &&
        targetAddresses != null) {
        log("[SafelistMaintenenceControler] Remove request " +
          targetAddresses.length + " addresses for account \"" + account + "\"");
        for(String addr : targetAddresses) {
          safelistContents = safelist.removeFromSafelist(account, addr);
        }
        msg = targetAddresses.length + " addresses removed from safelist";
      }
      else {
        log("[SafelistMaintenenceControler] View list request for account \"" + account + "\"");
        safelistContents = safelist.getSafelistContents(account);
      }

      SafelistListTag.setCurrentList(req, safelistContents);
      HasSafelistTag.setCurrent(req, true);

      if(msg != null) {
        MessagesSetTag.setInfoMessages(req, msg);
      }
      req.getRequestDispatcher(Constants.SAFELIST_VIEW).forward(req, resp);
      
    }
    catch(BadTokenException ex) {
      //Put a message in the request
      MessagesSetTag.setErrorMessages(req, "Unable to determine your email address from your previous request");
      req.getRequestDispatcher(Constants.REQ_DIGEST_VIEW).forward(req, resp);
      return;      
    }
    catch(NoSuchSafelistException ex) {
      //Odd case.  Someone had a valid auth token, yet
      //there is no safelist.  Likely, it was deleted.  Don't
      //give them an error - simply forward them to the display
      //page w/ no messages
      SafelistListTag.setCurrentList(req, null);
      req.getRequestDispatcher(Constants.SAFELIST_VIEW).forward(req, resp);
    }
//    catch(SafelistActionFailedException ex) {
//    }
    catch(Exception ex) {
      log("[SafelistMaintenenceControler] Exception servicing request", ex);
      req.getRequestDispatcher(Constants.SERVER_UNAVAILABLE_ERRO_VIEW).forward(req, resp);
      return;    
    }
    
  }

  private String base64DecodeAddress(String addr) {
    if(addr == null) {
      return null;
    }
    try {
      return new String(new BASE64Decoder().decodeBuffer(addr));
    }
    catch(Exception ex) {
      log("[SafelistMaintenenceControler] Exception base64 decoding \"" +
        addr + "\"");
      return null;
    }
  }
} 
