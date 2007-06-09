/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.node.mail.web.euv;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.node.mail.papi.quarantine.InboxIndex;
import com.untangle.node.mail.papi.quarantine.QuarantineUserView;
import com.untangle.node.mail.papi.safelist.NoSuchSafelistException;
import com.untangle.node.mail.papi.safelist.SafelistEndUserView;
import com.untangle.node.mail.web.euv.tags.HasSafelistTag;
import com.untangle.node.mail.web.euv.tags.MessagesSetTag;
import com.untangle.node.mail.web.euv.tags.SafelistListTag;
import com.untangle.node.util.Pair;

/**
 * Controler used for Safelist self-service maintenence
 */
public class SafelistMaintenenceControler
    extends MaintenenceControlerBase {

    protected void serviceImpl(HttpServletRequest req,
                               HttpServletResponse resp,
                               String account,
                               QuarantineUserView quarantine,
                               SafelistEndUserView safelist)
        throws ServletException, IOException {

        //Now, we wrap a bunch of calls in the same try/catch
        //block as the outcome based on exception type
        //is the same
        try {
            //Now, figure out what they wanted to do.  Either
            //sladd, slremove, or simply to see the safelist
            String action = req.getParameter(Constants.ACTION_RP);
            log("[SafelistMaintenenceControler] Action " + action);
            if(action == null) {
                action = Constants.SAFELIST_VIEW_RV;
            }

            //No matter what action we take, the outcome
            //is a summary of the safelist
            String[] safelistContents = null;
            String[] targetAddresses = req.getParameterValues(Constants.SAFELIST_TARGET_ADDR_RP);

            if(action.equals(Constants.SAFELIST_ADD_RV) &&
               targetAddresses != null
               && targetAddresses.length > 0) {

                Pair<String[], InboxIndex> result = addToSafelist(
                                                                  req, resp, account, quarantine, safelist, targetAddresses);
                safelistContents = result.a;

                MessagesSetTag.addInfoMessage(req, "Added " + targetAddresses.length + " address" + (targetAddresses.length > 1 ? "es" : "") + " to safelist");
            }
            else if(action.equals(Constants.SAFELIST_REMOVE_RV) &&
                    targetAddresses != null) {
                log("[SafelistMaintenenceControler] Removed " + targetAddresses.length + " address" + (targetAddresses.length > 1 ? "es" : "") + " for account \"" + account + "\"");
                for(String addr : targetAddresses) {
                    safelistContents = safelist.removeFromSafelist(account, addr);
                }
                MessagesSetTag.addInfoMessage(req, "Removed " + targetAddresses.length + " address" + (targetAddresses.length > 1 ? "es" : "") + " from safelist");
            }
            else {
                log("[SafelistMaintenenceControler] View list request for account \"" + account + "\"");
                safelistContents = safelist.getSafelistContents(account);
            }

            SafelistListTag.setCurrentList(req, safelistContents);
            HasSafelistTag.setCurrent(req, true);

            req.getRequestDispatcher(Constants.SAFELIST_VIEW).forward(req, resp);

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
}
