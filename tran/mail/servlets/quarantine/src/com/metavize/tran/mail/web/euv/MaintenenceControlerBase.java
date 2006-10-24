/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.mail.web.euv;

import java.io.IOException;
import java.util.HashSet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metavize.mvvm.addrbook.AddressBook;
import com.metavize.mvvm.addrbook.UserEntry;
import com.metavize.mvvm.portal.PortalLogin;
import com.metavize.mvvm.portal.PortalUser;
import com.metavize.tran.mail.papi.quarantine.BadTokenException;
import com.metavize.tran.mail.papi.quarantine.InboxIndex;
import com.metavize.tran.mail.papi.quarantine.InboxRecord;
import com.metavize.tran.mail.papi.quarantine.NoSuchInboxException;
import com.metavize.tran.mail.papi.quarantine.QuarantineUserActionFailedException;
import com.metavize.tran.mail.papi.quarantine.QuarantineUserView;
import com.metavize.tran.mail.papi.safelist.NoSuchSafelistException;
import com.metavize.tran.mail.papi.safelist.SafelistActionFailedException;
import com.metavize.tran.mail.papi.safelist.SafelistEndUserView;
import com.metavize.tran.mail.web.euv.tags.CurrentAuthTokenTag;
import com.metavize.tran.mail.web.euv.tags.CurrentEmailAddressTag;
import com.metavize.tran.mail.web.euv.tags.IsReceivesRemapsTag;
import com.metavize.tran.mail.web.euv.tags.IsRemappedTag;
import com.metavize.tran.mail.web.euv.tags.MessagesSetTag;
import com.metavize.tran.mail.web.euv.tags.ReceivingRemapsListTag;
import com.metavize.tran.mail.web.euv.tags.RemappedToTag;
import com.metavize.tran.util.Pair;
import org.apache.log4j.Logger;

/**
 * Base class for common controler functionality
 */
public abstract class MaintenenceControlerBase
    extends HttpServlet {

    Logger logger = Logger.getLogger(getClass());

    protected void service(HttpServletRequest req,
                           HttpServletResponse resp)
        throws ServletException, IOException {

        String authTkn = req.getParameter(Constants.AUTH_TOKEN_RP);
        if(authTkn == null) {
            log("[MaintenenceControlerBase] Auth token null");
            req.getRequestDispatcher(Constants.REQ_DIGEST_VIEW).forward(req, resp);
            return;
        }

        //Get the QuarantineUserView reference.  If we cannot,
        //the user is SOL
        SafelistEndUserView safelist =
            QuarantineEnduserServlet.instance().getSafelist();
        if(safelist == null) {
            log("[MaintenenceControlerBase] Safelist Hosed");
            req.getRequestDispatcher(Constants.SERVER_UNAVAILABLE_ERRO_VIEW).forward(req, resp);
            return;
        }
        QuarantineUserView quarantine =
            QuarantineEnduserServlet.instance().getQuarantine();
        if(quarantine == null) {
            log("[MaintenenceControlerBase] Quarantine Hosed");
            req.getRequestDispatcher(Constants.SERVER_UNAVAILABLE_ERRO_VIEW).forward(req, resp);
            return;
        }

        String account = null;
        try {
            //Attempt to decrypt their token
            if (authTkn.equals("PU")) {
                account = "amread@metavize.com";
            } else {
                account = quarantine.getAccountFromToken(authTkn);
            }
            String remappedTo = quarantine.getMappedTo(account);
            IsRemappedTag.setCurrent(req, remappedTo!= null);
            if(remappedTo != null) {
                RemappedToTag.setCurrent(req, remappedTo);
            }

            String[] inboundRemappings = quarantine.getMappedFrom(account);
            if(inboundRemappings != null && inboundRemappings.length > 0) {
                IsReceivesRemapsTag.setCurrent(req, true);
                ReceivingRemapsListTag.setCurrentList(req, inboundRemappings);
            }
            else {
                IsReceivesRemapsTag.setCurrent(req, false);
            }
        }
        catch(BadTokenException ex) {
            //Put a message in the request
            MessagesSetTag.setErrorMessages(req, "Unable to determine your email address from your previous request");
            req.getRequestDispatcher(Constants.REQ_DIGEST_VIEW).forward(req, resp);
            return;
        }
        catch(Exception ex) {
            log("[MaintenenceControlerBase] Exception servicing request", ex);
            req.getRequestDispatcher(Constants.SERVER_UNAVAILABLE_ERRO_VIEW).forward(req, resp);
            return;
        }
        CurrentEmailAddressTag.setCurrent(req, account);
        CurrentAuthTokenTag.setCurrent(req, authTkn);

        serviceImpl(req, resp, account, quarantine, safelist);
    }

    protected abstract void serviceImpl(HttpServletRequest req,
                                        HttpServletResponse resp,
                                        String account,
                                        QuarantineUserView quarantine,
                                        SafelistEndUserView safelist)
        throws ServletException, IOException;

    /**
     * Adds any messages to the message set
     */
    protected Pair<String[], InboxIndex> addToSafelist(HttpServletRequest req,
                                                       HttpServletResponse resp,
                                                       String thisUserAccount,
                                                       QuarantineUserView quarantine,
                                                       SafelistEndUserView safelist,
                                                       String[] addressesToSafelist)
        throws ServletException,
               IOException,
               NoSuchSafelistException,
               SafelistActionFailedException,
               NoSuchInboxException,
               QuarantineUserActionFailedException {

        if(addressesToSafelist == null ||
           addressesToSafelist.length == 0) {
            return new Pair<String[], InboxIndex>(
                                                  safelist.getSafelistContents(thisUserAccount),
                                                  quarantine.getInboxIndex(thisUserAccount));
        }

        String[] addresses = new String[addressesToSafelist.length];
        for(int i = 0; i<addresses.length; i++) {
            addresses[i] = addressesToSafelist[i].toLowerCase();
        }

        String[] safelistToReturn = null;

        for(String addr : addresses) {
            safelistToReturn = safelist.addToSafelist(thisUserAccount, addr);
        }

        InboxIndex index = null;

        //Now, find any messages to release
        HashSet<String> midsToRelease =
            new HashSet<String>();
        index = quarantine.getInboxIndex(thisUserAccount);
        for(InboxRecord record : index) {
            for(String addr : addresses) {
                if(record.getMailSummary().getSender().equalsIgnoreCase(addr)) {
                    midsToRelease.add(record.getMailID());
                }
            }
        }
        if(midsToRelease.size() > 0) {
            index = quarantine.rescue(thisUserAccount,
                                      (String[]) midsToRelease.toArray(new String[midsToRelease.size()]));
            MessagesSetTag.addInfoMessage(req, "Released "+ midsToRelease.size() + " message" + (midsToRelease.size() > 1 ? "s" : "") + " from safelisted senders");
        }

        return new Pair<String[], InboxIndex>(safelistToReturn, index);
    }
}
