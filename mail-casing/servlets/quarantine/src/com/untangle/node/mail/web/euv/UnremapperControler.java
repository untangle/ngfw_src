/*
 * $HeadURL:$
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
