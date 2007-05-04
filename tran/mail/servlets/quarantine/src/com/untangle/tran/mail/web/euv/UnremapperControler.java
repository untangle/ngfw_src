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
package com.untangle.tran.mail.web.euv;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.tran.mail.papi.quarantine.QuarantineUserView;
import com.untangle.tran.mail.papi.safelist.SafelistEndUserView;
import com.untangle.tran.mail.web.euv.tags.IsReceivesRemapsTag;
import com.untangle.tran.mail.web.euv.tags.IsRemappedTag;
import com.untangle.tran.mail.web.euv.tags.MessagesSetTag;
import com.untangle.tran.mail.web.euv.tags.ReceivingRemapsListTag;
import com.untangle.tran.mail.web.euv.tags.RemappedToTag;



/**
 * Controler used for remapping (into) self-service maintenence
 */
public class UnremapperControler
    extends MaintenenceControlerBase {

    protected void serviceImpl(HttpServletRequest req,
                               HttpServletResponse resp,
                               String account,
                               QuarantineUserView quarantine,
                               SafelistEndUserView safelist)
        throws ServletException, IOException {

        try {

            //Now, figure out what they wanted to do.  Either
            //sladd, slremove, or simply to see the safelist
            String action = req.getParameter(Constants.ACTION_RP);
            log("[UnremapperControler] Action " + action);
            if(action == null) {
                action = Constants.UNMAPPER_VIEW_RV;
            }

            String[] targetAddresses = req.getParameterValues(Constants.UNMAPPER_TARGET_ADDR_RP);

            if(action.equals(Constants.UNMAPPER_REMOVE_RV) &&
               targetAddresses != null) {
                log("[UnremapperControler] Remove request " +
                    targetAddresses.length + " addresses for account \"" + account + "\"");

                for(String addr : targetAddresses) {
                    quarantine.unmapSelfService(account, addr);
                }
                MessagesSetTag.addInfoMessage(req,
                                              targetAddresses.length + " addresses no longer redirecting to \"" + account + "\"");
            }
            else {
                log("[UnremapperControler] View list request for account \"" + account + "\"");
            }

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

            req.getRequestDispatcher(Constants.UNMAP_ADDRESS_VIEW).forward(req, resp);

        }
        catch(Exception ex) {
            log("[UnremapperControler] Exception servicing request", ex);
            req.getRequestDispatcher(Constants.SERVER_UNAVAILABLE_ERRO_VIEW).forward(req, resp);
            return;
        }
    }
}
