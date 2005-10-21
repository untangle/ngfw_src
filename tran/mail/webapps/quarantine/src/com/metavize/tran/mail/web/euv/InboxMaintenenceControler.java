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

import com.metavize.tran.mail.papi.quarantine.QuarantineUserView;
import com.metavize.tran.mail.papi.quarantine.InboxIndex;
import com.metavize.tran.mail.papi.quarantine.InboxRecord;
import com.metavize.tran.mail.papi.quarantine.BadTokenException;
import com.metavize.tran.mail.papi.quarantine.QuarantineUserActionFailedException;
import com.metavize.tran.mail.papi.quarantine.NoSuchInboxException;
import com.metavize.tran.mail.papi.quarantine.InboxRecordComparator;
import com.metavize.tran.mail.papi.quarantine.InboxRecordCursor;

import com.metavize.tran.mail.web.euv.tags.MessagesSetTag;
import com.metavize.tran.mail.web.euv.tags.CurrentEmailAddressTag;
import com.metavize.tran.mail.web.euv.tags.InboxIndexTag;
import com.metavize.tran.mail.web.euv.tags.CurrentAuthTokenTag;

/**
 * Controler used for inbox maintenence (purge/rescue/view).
 */
public class InboxMaintenenceControler
  extends HttpServlet {

  protected void service(HttpServletRequest req,
    HttpServletResponse resp)
    throws ServletException, IOException {

    //Check for an auth token.  If there is none,
    //then implicitly forward them to the "request login"
    //page.  For now, do not send along an error
    //as the text would be unclear ("...unable to find encrypted
    //auth token in your URL...").
    String authTkn = req.getParameter(Constants.AUTH_TOKEN_RP);
    if(authTkn == null) {
      log("[InboxMaintenenceControler] Auth token null");
      req.getRequestDispatcher(Constants.REQ_DIGEST_VIEW).forward(req, resp);
      return;
    }

    //Get the QuarantineUserView reference.  If we cannot,
    //the user is SOL
    QuarantineUserView quarantine =
      QuarantineEnduserServlet.instance().getQuarantine();
    if(quarantine == null) {
      log("[InboxMaintenenceControler] Quarantine Hosed");
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
      //purge, rescue, or simply to see the index
      String action = req.getParameter(Constants.ACTION_RP);
      log("[InboxMaintenenceControler] Action " + action);
      if(action == null) {
        action = Constants.VIEW_INBOX_RV;
      }

      //Defaults for the view of the user's list.
      InboxRecordComparator.SortBy sortBy = InboxRecordComparator.SortBy.INTERN_DATE;
      boolean ascending = true;
      int startingAt = 0;

      if(req.getParameter(Constants.FIRST_RECORD_RP) != null) {
        try {
          startingAt = Integer.parseInt(req.getParameter(Constants.FIRST_RECORD_RP));
        }
        catch(Exception ex) {
        }
      }

      if(req.getParameter(Constants.SORT_ASCEND_RP) != null) {
        try {
          ascending = Boolean.parseBoolean(req.getParameter(Constants.SORT_ASCEND_RP));
        }
        catch(Exception ex) {
        }
      }
      
      if(req.getParameter(Constants.SORT_BY_RP) != null) {
        try {
          sortBy = Util.stringToSortBy(req.getParameter(Constants.SORT_BY_RP), sortBy);
        }
        catch(Exception ex) {
        }
      }         
      

      //A "result" message, which may be displayed if
      //they requested an action
      String msg = null;

      //No matter what action we take, the outcome
      //is an Index
      InboxIndex index = null;

      String[] mids = req.getParameterValues(Constants.MAIL_ID_RP);

      log("[InboxMaintenenceControler]" + (mids==null?"0":mids.length) + " Mail IDs");
      
      if(action.equals(Constants.PURGE_RV) &&
        mids!= null && mids.length > 0) {
        log("[InboxMaintenenceControler] Purge request for account \"" + account + "\"");
        index = quarantine.purge(account, mids);
        msg = "Message" + (mids.length>0?"s":"") + " purged";
      }
      else if(action.equals(Constants.RESCUE_RV) &&
        mids!= null && mids.length > 0) {
        log("[InboxMaintenenceControler] Rescue request for account \"" + account + "\"");
        index = quarantine.rescue(account, mids);
        msg = "Message" + (mids.length>0?"s":"") + " released";
      }
      else {
        log("[InboxMaintenenceControler] Inbox View request for account \"" + account + "\"");
        index = quarantine.getInboxIndex(account);
      }

      InboxIndexTag.setCurrentIndex(req,
        InboxRecordCursor.get(
          index.getAllRecords(),
          sortBy,
          ascending,
          startingAt,
          Constants.RECORDS_PER_PAGE));

      if(msg != null) {
        MessagesSetTag.setInfoMessages(req, msg);
      }
      req.getRequestDispatcher(Constants.INBOX_VIEW).forward(req, resp);
      
    }
    catch(BadTokenException ex) {
      //Put a message in the request
      MessagesSetTag.setErrorMessages(req, "Unable to determine inbox from your previous request");
      req.getRequestDispatcher(Constants.REQ_DIGEST_VIEW).forward(req, resp);
      return;      
    }
    catch(NoSuchInboxException ex) {
      //Odd case.  Someone had a valid auth token, yet
      //there is no inbox.  Likely, it was deleted.  Don't
      //give them an error - simply forward them to the display
      //page w/ no messages
      InboxIndexTag.setCurrentIndex(req, null);
      req.getRequestDispatcher(Constants.INBOX_VIEW).forward(req, resp);
    }
//    catch(QuarantineUserActionFailedException ex) {
//    }
    catch(Exception ex) {
      log("[InboxMaintenenceControler] Exception servicing request", ex);
      req.getRequestDispatcher(Constants.SERVER_UNAVAILABLE_ERRO_VIEW).forward(req, resp);
      return;    
    }

  }
} 
