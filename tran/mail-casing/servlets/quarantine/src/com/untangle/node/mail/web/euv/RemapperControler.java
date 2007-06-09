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

import com.untangle.node.mail.papi.quarantine.QuarantineUserView;
import com.untangle.node.mail.papi.safelist.SafelistEndUserView;
import com.untangle.node.mail.web.euv.tags.IsReceivesRemapsTag;
import com.untangle.node.mail.web.euv.tags.IsRemappedTag;
import com.untangle.node.mail.web.euv.tags.MessagesSetTag;
import com.untangle.node.mail.web.euv.tags.ReceivingRemapsListTag;
import com.untangle.node.mail.web.euv.tags.RemappedToTag;



/**
 * Controler used for delegating the quarantining of one
 * email address into the Inbox of another.
 */
public class RemapperControler
    extends MaintenenceControlerBase {

    protected void serviceImpl(HttpServletRequest req,
                               HttpServletResponse resp,
                               String account,
                               QuarantineUserView quarantine,
                               SafelistEndUserView safelist)
        throws ServletException, IOException {

        try {

            String action = req.getParameter(Constants.ACTION_RP);
            log("[RemapperControler] Action " + action);
            if(action == null) {
                action = Constants.MAPPER_VIEW_RV;
            }

            if(action.equals(Constants.MAPPER_DO_REMAP_RV)) {

                String remapTo = req.getParameter(Constants.MAPPER_TARGET_ADDR_RP);

                if(remapTo == null || "".equals(remapTo.trim())) {
                    MessagesSetTag.addErrorMessage(req,
                                                   "Please provide the email address to forward quarantines into");
                    req.getRequestDispatcher(Constants.MAP_ADDRESS_VIEW).forward(req, resp);
                    return;
                }

                quarantine.remapSelfService(account, remapTo);
                MessagesSetTag.addInfoMessage(req,
                                              "Quarantine email for \"" + account + "\" will be redirected to \"" + remapTo + "\"");


                //Fixup the display properties
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

                req.getRequestDispatcher("manageuser").forward(req, resp);
            }
            else {
                //Nothing to do
                req.getRequestDispatcher(Constants.MAP_ADDRESS_VIEW).forward(req, resp);
            }
        }
        catch(Exception ex) {
            log("[RemapperControler] Exception servicing request", ex);
            req.getRequestDispatcher(Constants.SERVER_UNAVAILABLE_ERRO_VIEW).forward(req, resp);
            return;
        }
    }
}
