/**
 * $Id: InboxMaintenenceControler.java 34290 2013-03-17 00:00:19Z dmorris $
 */
package com.untangle.node.smtp.web.euv;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.node.smtp.quarantine.BadTokenException;
import com.untangle.node.smtp.quarantine.InboxRecord;
import com.untangle.node.smtp.quarantine.NoSuchInboxException;
import com.untangle.node.smtp.quarantine.QuarantineUserView;
import com.untangle.node.smtp.safelist.SafelistManipulation;
import com.untangle.node.smtp.web.euv.tags.CurrentAuthTokenTag;
import com.untangle.node.smtp.web.euv.tags.CurrentEmailAddressTag;
import com.untangle.node.smtp.web.euv.tags.HasSafelistTag;
import com.untangle.node.smtp.web.euv.tags.IsReceivesRemapsTag;
import com.untangle.node.smtp.web.euv.tags.IsRemappedTag;
import com.untangle.node.smtp.web.euv.tags.MaxDaysIdleInboxTag;
import com.untangle.node.smtp.web.euv.tags.MaxDaysToInternTag;
import com.untangle.node.smtp.web.euv.tags.QuarantineFunctions;
import com.untangle.node.smtp.web.euv.tags.RemappedToTag;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;

/**
 * Controler used for inbox maintenence (purge/rescue/refresh/view).
 */
@SuppressWarnings("serial")
public class InboxMaintenenceControler extends HttpServlet
{
    protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {

        String authTkn = req.getParameter(Constants.AUTH_TOKEN_RP);
        if(authTkn == null) {
            log("[MaintenenceControlerBase] Auth token null");
            req.getRequestDispatcher(Constants.REQ_DIGEST_VIEW).forward(req, resp);
            return;
        }

        //Get the QuarantineUserView reference.  If we cannot,
        //the user is SOL
        SafelistManipulation safelist =
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
        String maxDaysToIntern =
            QuarantineEnduserServlet.instance().getMaxDaysToIntern();
        if(maxDaysToIntern == null) {
            log("[MaintenenceControlerBase] Quarantine Settings (days to intern) Hosed");
            req.getRequestDispatcher(Constants.SERVER_UNAVAILABLE_ERRO_VIEW).forward(req, resp);
            return;
        }
        String maxDaysIdleInbox =
            QuarantineEnduserServlet.instance().getMaxDaysIdleInbox();
        if(maxDaysIdleInbox == null) {
            log("[MaintenenceControlerBase] Quarantine Settings (days inbox idle) Hosed");
            req.getRequestDispatcher(Constants.SERVER_UNAVAILABLE_ERRO_VIEW).forward(req, resp);
            return;
        }

        String account = null;
        try {
            account = quarantine.getAccountFromToken(authTkn);

            String remappedTo = quarantine.getMappedTo(account);
            IsRemappedTag.setCurrent(req, remappedTo!= null);
            if(remappedTo != null) {
                RemappedToTag.setCurrent(req, remappedTo);
            }

            String[] inboundRemappings = quarantine.getMappedFrom(account);
            if(inboundRemappings != null && inboundRemappings.length > 0) {
                IsReceivesRemapsTag.setCurrent(req, true);
                QuarantineFunctions.setCurrentRemapsList(req, inboundRemappings);
            }
            else {
                IsReceivesRemapsTag.setCurrent(req, false);
            }
        }
        catch(BadTokenException ex) {
            //Put a message in the request
            QuarantineFunctions.setErrorMessages(req, "Unable to determine your email address from your previous request");
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
        MaxDaysToInternTag.setMaxDays(req, maxDaysToIntern);
        MaxDaysIdleInboxTag.setMaxDays(req, maxDaysIdleInbox);

        /* This is just plain wrong. */
        req.setAttribute( "cdata_start", "<![CDATA[" );
        req.setAttribute( "cdata_end", "]]>" );
        /* End of the wrongness */


        /* Setup the cobranding settings. */
        UvmContext uvm = UvmContextFactory.context();
        req.setAttribute( "companyName", uvm.brandingManager().getCompanyName());
        req.setAttribute( "companyUrl", uvm.brandingManager().getCompanyUrl());
        
        /* setup the skinning settings */
        req.setAttribute( "skinSettings", uvm.skinManager().getSettings());
        serviceImpl(req, resp, account, quarantine, safelist);
    }

    protected void serviceImpl(HttpServletRequest req, HttpServletResponse resp, String account, QuarantineUserView quarantine, SafelistManipulation safelist)
        throws ServletException, IOException
    {

        try {
            //Now, figure out what they wanted to do.  Either
            //purge, rescue, refresh, or simply to see the index
            //Note that refresh or simply to see the index,
            //we fall through and take the basic action (not purge and rescue)

            //No matter what action we take, the outcome is an Index
            List<InboxRecord> inboxRecords = quarantine.getInboxRecords(account);

            //Set the flag for whether the user does/does not have a Safelist
            HasSafelistTag.setCurrent(req, safelist.hasOrCanHaveSafelist(account));

            QuarantineFunctions.setCurrentSafelist(req, safelist.getSafelistContents(account));

            QuarantineFunctions.setCurrentIndex(req, inboxRecords.toArray(new InboxRecord[0]));

            req.getRequestDispatcher(Constants.INBOX_VIEW).forward(req, resp);
        } catch(NoSuchInboxException ex) {
            //Odd case.  Someone had a valid auth token, yet
            //there is no inbox.  Likely, it was deleted.  Don't
            //give them an error - simply forward them to the display
            //page w/ no messages
            QuarantineFunctions.setCurrentIndex(req, null);
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
