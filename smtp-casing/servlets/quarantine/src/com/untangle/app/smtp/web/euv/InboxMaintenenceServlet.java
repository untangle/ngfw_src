/**
 * $Id: InboxMaintenenceControler.java 36445 2013-11-20 00:04:22Z dmorris $
 */
package com.untangle.app.smtp.web.euv;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;

import com.untangle.app.smtp.quarantine.BadTokenException;
import com.untangle.app.smtp.quarantine.QuarantineUserView;
import com.untangle.app.smtp.safelist.SafelistManipulation;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;

/**
 * Servlet used for inbox maintenence.
 */
@SuppressWarnings("serial")
public class InboxMaintenenceServlet extends HttpServlet
{
    /**
     * Page to forward end-users to, if the system is
     * hosed and cannot fufill request.
     */
    private static final String AUTH_TOKEN_RP = "tkn";
    private static final String INBOX_VIEW = "/WEB-INF/jsp/inbox.jsp";
    private static final String REQUEST_FWD = "/request";
    private static final String SERVER_UNAVAILABLE_ERRO_VIEW = "/TryLater.jsp";

    /**
     * Setup up the service for the user specified in the "tkn" request parameter.
     * If the token is invlaid or we're unable to access the appropriate quarantine, the request
     * will be redirected to handle the issue.
     * 
     * @param  req              HttpServletRequest object containing the action "tkn" parameter.
     * @param  resp             HttpServletResponse object that will be used in any redirecion.
     * @throws ServletException If there's an problem with the servlet.
     * @throws IOException      General input/ooutput error.
     */
    protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {

        String authToken = req.getParameter(AUTH_TOKEN_RP);

        if(authToken == null) {
            log("[MaintenenceControlerBase] Auth token null");
            req.getRequestDispatcher(REQUEST_FWD).forward(req, resp);
            return;
        }

        //Get the QuarantineUserView reference.  If we cannot, the user is SOL
        SafelistManipulation safelistManipulation = QuarantineEnduserServlet.instance().getSafelist();
        if(safelistManipulation == null) {
            log("[MaintenenceControlerBase] Safelist Hosed");
            req.getRequestDispatcher(SERVER_UNAVAILABLE_ERRO_VIEW).forward(req, resp);
            return;
        }
        QuarantineUserView quarantine = QuarantineEnduserServlet.instance().getQuarantine();
        if(quarantine == null) {
            log("[MaintenenceControlerBase] Quarantine Hosed");
            req.getRequestDispatcher(SERVER_UNAVAILABLE_ERRO_VIEW).forward(req, resp);
            return;
        }
        String maxDaysToIntern =
            QuarantineEnduserServlet.instance().getMaxDaysToIntern();
        if(maxDaysToIntern == null) {
            log("[MaintenenceControlerBase] Quarantine Settings (days to intern) Hosed");
            req.getRequestDispatcher(SERVER_UNAVAILABLE_ERRO_VIEW).forward(req, resp);
            return;
        }

        String account = null;
        try {
            if("test".equals(authToken)) { //Just for ui testing
                account="test@untangle.com";
                req.setAttribute( "forwardAddress", "remapped@unatangle.com");
                req.setAttribute( "safelistData", buildJsonList(new String[] {"safeOne@test.com", "safeTwo@test.com"}));
                req.setAttribute( "remapsData", "[]");
            } else {
                account = quarantine.getAccountFromToken(authToken);

                String remappedTo = quarantine.getMappedTo(account);
                if(remappedTo != null) {
                    req.setAttribute( "forwardAddress", remappedTo);
                }

                String[] inboundRemappings = quarantine.getMappedFrom(account);
                req.setAttribute( "remapsData", buildJsonList(inboundRemappings));

                String[] safelistData = safelistManipulation.getSafelistContents(account);
                req.setAttribute( "safelistData", buildJsonList(safelistData));
            }
        }
        catch(BadTokenException ex) {
            req.getRequestDispatcher(REQUEST_FWD).forward(req, resp);
            return;
        }
        catch(Exception ex) {
            log("[MaintenenceControlerBase] Exception servicing request", ex);
            req.getRequestDispatcher(SERVER_UNAVAILABLE_ERRO_VIEW).forward(req, resp);
            return;
        }

        req.setAttribute( "currentAddress", account);
        req.setAttribute( "currentAuthToken", authToken);
        req.setAttribute( "quarantineDays", maxDaysToIntern);

        /* Setup the cobranding settings. */
        UvmContext uvm = UvmContextFactory.context();
        req.setAttribute( "companyName", uvm.brandingManager().getCompanyName());

        /* setup the skinning settings */
        req.setAttribute( "skinName", uvm.skinManager().getSettings().getSkinName());
        req.setAttribute( "extjsTheme", uvm.skinManager().getSkinInfo().getExtjsTheme());
        req.getRequestDispatcher(INBOX_VIEW).forward(req, resp);
    }

    /**
     * Build a JSON list from the array of values.
     * 
     * @param  values Array of Strings.
     * @return        String representing a JSON list.
     */
    private static final String buildJsonList( String[] values )
    {
        if ( values == null || values.length == 0 ) {
            return "[]";
        }

        JSONArray ja = new JSONArray();
        for ( String value : values ) {
            JSONArray v = new JSONArray();
            v.put( value );
            ja.put( v );
        }

        return ja.toString();
    }
}
