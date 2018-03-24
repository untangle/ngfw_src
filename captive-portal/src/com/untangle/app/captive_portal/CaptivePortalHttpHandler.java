/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import org.apache.log4j.Logger;

import com.untangle.app.http.HttpEventHandler;
import com.untangle.app.http.RequestLineToken;
import com.untangle.app.http.StatusLine;
import com.untangle.uvm.vnet.EndMarkerToken;
import com.untangle.app.http.HeaderToken;
import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.AppTCPSession;

/**
 * This is the handler for all HTTP traffic.
 * 
 * @author mahotz
 * 
 */

public class CaptivePortalHttpHandler extends HttpEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());
    private final CaptivePortalApp app;

    /**
     * Our contructor
     * 
     * @param app
     *        The application instance that created us
     */
    CaptivePortalHttpHandler(CaptivePortalApp app)
    {
        super();
        this.app = app;
    }

    /**
     * Main handler for HTTP requests allows traffic for authenticated users,
     * hosts in pass lists, or anything that matches a pass rule. We also allow
     * certain hosts to make OAuth work better, and return 503 for prefetch
     * requests because it causes problems with the capture page.
     * 
     * 
     * @param session
     *        The session object
     * @param requestHeader
     *        The request header
     * @return
     */
    @Override
    protected HeaderToken doRequestHeader(AppTCPSession session, HeaderToken requestHeader)
    {
        Token[] response = null;
        boolean allowed = false;

        // first check to see if the user is already authenticated
        if (app.isClientAuthenticated(session.getClientAddr()) == true) {
            app.incrementBlinger(CaptivePortalApp.BlingerType.SESSALLOW, 1);
            releaseRequest(session);
            return (requestHeader);
        }

        // not authenticated so check both of the pass lists
        PassedAddress passed = app.isSessionAllowed(session.getClientAddr(), session.getServerAddr());

        if (passed != null) {
            if (passed.getLog() == true) {
                CaptureRuleEvent logevt = new CaptureRuleEvent(session.sessionEvent(), false);
                app.logEvent(logevt);
            }

            app.incrementBlinger(CaptivePortalApp.BlingerType.SESSALLOW, 1);
            releaseRequest(session);
            return (requestHeader);
        }

        // not authenticated and no pass list match so check the rules
        CaptureRule rule = app.checkCaptureRules(session);

        // by default we allow traffic so if there is no rule pass the traffic
        if (rule == null) {
            app.incrementBlinger(CaptivePortalApp.BlingerType.SESSALLOW, 1);
            releaseRequest(session);
            return (requestHeader);
        }

        // if we found a pass rule then log and let the traffic pass
        if (rule.getCapture() == false) {
            CaptureRuleEvent logevt = new CaptureRuleEvent(session.sessionEvent(), rule);
            app.logEvent(logevt);

            app.incrementBlinger(CaptivePortalApp.BlingerType.SESSALLOW, 1);
            releaseRequest(session);
            return (requestHeader);
        }

        String method = getRequestLine(session).getMethod().toString();
        String uri = getRequestLine(session).getRequestUri().toString();

        // look for a host in the request line
        String host = getRequestLine(session).getRequestUri().getHost();

        // if not found there look in the request header
        if (host == null) host = requestHeader.getValue("Host");

        // if still not found then just use the IP address of the server
        if (host == null) host = session.getServerAddr().getHostAddress().toString();

        // always convert to lower case
        host = host.toLowerCase();

        // allow things needed to make OAuth login work better only when enabled
        CaptivePortalSettings.AuthenticationType authType = app.getSettings().getAuthenticationType();
        if ((authType == CaptivePortalSettings.AuthenticationType.GOOGLE) || (authType == CaptivePortalSettings.AuthenticationType.FACEBOOK) || (authType == CaptivePortalSettings.AuthenticationType.MICROSOFT) || (authType == CaptivePortalSettings.AuthenticationType.ANY_OAUTH)) {

            // google devices hit this before the user requested URL which breaks the post authentication redirect to the original destination 
            if (host.equals("connectivitycheck.gstatic.com")) allowed = true;

            if (allowed == true) {
                logger.info("Releasing HTTP OAuth session: " + host);
                releaseRequest(session);
                return (requestHeader);
            }
        }

        // look for prefetch shenaniganery
        String prefetch = requestHeader.getValue("X-moz");

        // found a prefetch request so return a special error response
        if ((prefetch != null) && (prefetch.contains("prefetch") == true)) {
            response = generatePrefetchResponse();
        }

        // not a prefetch so generate the captive portal redirect
        else {
            CaptivePortalBlockDetails details = new CaptivePortalBlockDetails(host, uri, method);
            response = app.generateResponse(details, session);
        }

        CaptureRuleEvent logevt = new CaptureRuleEvent(session.sessionEvent(), rule);
        app.logEvent(logevt);
        app.incrementBlinger(CaptivePortalApp.BlingerType.SESSBLOCK, 1);
        blockRequest(session, response);
        return requestHeader;
    }

    /**
     * Request body handler
     * 
     * @param session
     *        The session
     * 
     * @param chunk
     *        The chunk token
     * 
     * @return The chunk token
     */
    @Override
    protected ChunkToken doRequestBody(AppTCPSession session, ChunkToken chunk)
    {
        return chunk;
    }

    /**
     * Request body end handler
     * 
     * @param session
     *        The session
     */
    @Override
    protected void doRequestBodyEnd(AppTCPSession session)
    {
    }

    /**
     * The response header handler
     * 
     * @param session
     *        The session
     * @param header
     *        The header token
     * @return The header token
     */
    @Override
    protected HeaderToken doResponseHeader(AppTCPSession session, HeaderToken header)
    {
        releaseResponse(session);
        return header;
    }

    /**
     * Response body handler
     * 
     * @param session
     *        The session
     * @param chunk
     *        The chunk token
     * @return The chunk token
     */
    @Override
    protected ChunkToken doResponseBody(AppTCPSession session, ChunkToken chunk)
    {
        return chunk;
    }

    /**
     * The response body end handler
     * 
     * @param session
     *        The session
     */
    @Override
    protected void doResponseBodyEnd(AppTCPSession session)
    {
    }

    /**
     * The quest line handler
     * 
     * @param session
     *        The session
     * 
     * @param requestLine
     *        The request line token
     * @return The request line token
     */
    @Override
    protected RequestLineToken doRequestLine(AppTCPSession session, RequestLineToken requestLine)
    {
        return requestLine;
    }

    /**
     * The status line handler
     * 
     * @param session
     *        The session
     * @param statusLine
     *        The status line
     * @return The status line
     */
    @Override
    protected StatusLine doStatusLine(AppTCPSession session, StatusLine statusLine)
    {
        return statusLine;
    }

    /**
     * Generate a forbidden response for prefetch queries
     * 
     * @return Token containing the 503 response code
     */
    private Token[] generatePrefetchResponse()
    {
        Token response[] = new Token[4];

        StatusLine sl = new StatusLine("HTTP/1.1", 503, "Service Unavailable");
        response[0] = sl;

        HeaderToken head = new HeaderToken();
        head.addField("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
        head.addField("Pragma", "no-cache");
        head.addField("Expires", "Mon, 10 Jan 2000 00:00:00 GMT");
        head.addField("Content-Length", "0");
        head.addField("Connection", "Close");
        response[1] = head;

        response[2] = ChunkToken.EMPTY;
        response[3] = EndMarkerToken.MARKER;
        return (response);
    }
}
