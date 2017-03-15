/**
 * $Id$
 */

package com.untangle.node.captive_portal;

import java.net.InetAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.untangle.node.http.HttpEventHandler;
import com.untangle.node.http.HttpMethod;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.RequestLine;
import com.untangle.node.http.StatusLine;
import com.untangle.uvm.vnet.EndMarkerToken;
import com.untangle.node.http.HeaderToken;
import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.AppTCPSession;

public class CaptivePortalHttpHandler extends HttpEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());
    private final CaptivePortalApp node;

    // constructors -----------------------------------------------------------

    CaptivePortalHttpHandler( CaptivePortalApp node )
    {
        super();
        this.node = node;
    }

    // HttpEventHandler methods -----------------------------------------------

    @Override
    protected HeaderToken doRequestHeader( AppTCPSession session, HeaderToken requestHeader )
    {
        Token[] response = null;

        // first check to see if the user is already authenticated
        if (node.isClientAuthenticated(session.getClientAddr()) == true) {
            node.incrementBlinger(CaptivePortalApp.BlingerType.SESSALLOW, 1);
            releaseRequest( session );
            return (requestHeader);
        }

        // not authenticated so check both of the pass lists
        PassedAddress passed = node.isSessionAllowed(session.getClientAddr(), session.getServerAddr());

        if (passed != null) {
            if (passed.getLog() == true) {
                CaptureRuleEvent logevt = new CaptureRuleEvent(session.sessionEvent(), false);
                node.logEvent(logevt);
            }

            node.incrementBlinger(CaptivePortalApp.BlingerType.SESSALLOW, 1);
            releaseRequest( session );
            return (requestHeader);
        }

        // not authenticated and no pass list match so check the rules
        CaptureRule rule = node.checkCaptureRules(session);

        // by default we allow traffic so if there is no rule pass the traffic
        if (rule == null) {
            node.incrementBlinger(CaptivePortalApp.BlingerType.SESSALLOW, 1);
            releaseRequest( session );
            return (requestHeader);
        }

        // if we found a pass rule then log and let the traffic pass
        if (rule.getCapture() == false) {
            CaptureRuleEvent logevt = new CaptureRuleEvent(session.sessionEvent(), rule);
            node.logEvent(logevt);

            node.incrementBlinger(CaptivePortalApp.BlingerType.SESSALLOW, 1);
            releaseRequest( session );
            return (requestHeader);
        }

        String method = getRequestLine( session ).getMethod().toString();
        String uri = getRequestLine( session ).getRequestUri().toString();

        // look for a host in the request line
        String host = getRequestLine( session ).getRequestUri().getHost();

        // if not found there look in the request header
        if (host == null)
            host = requestHeader.getValue("Host");

        // if still not found then just use the IP address of the server
        if (host == null)
            host = session.getServerAddr().getHostAddress().toString();

        host = host.toLowerCase();

        // look for prefetch shenaniganery
        String prefetch = requestHeader.getValue("X-moz");

        // found a prefetch request so return a special error response
        if ((prefetch != null) && (prefetch.contains("prefetch") == true)) {
            response = generatePrefetchResponse();
        }

        // not a prefetch so generate the captive portal redirect
        else {
            CaptivePortalBlockDetails details = new CaptivePortalBlockDetails(host, uri, method);
            response = node.generateResponse(details, session);
        }

        CaptureRuleEvent logevt = new CaptureRuleEvent(session.sessionEvent(), rule);
        node.logEvent(logevt);
        node.incrementBlinger(CaptivePortalApp.BlingerType.SESSBLOCK, 1);
        blockRequest( session, response );
        return requestHeader;
    }

    @Override
    protected ChunkToken doRequestBody( AppTCPSession session, ChunkToken chunk )
    {
        return chunk;
    }

    @Override
    protected void doRequestBodyEnd( AppTCPSession session )
    {
    }

    @Override
    protected HeaderToken doResponseHeader( AppTCPSession session, HeaderToken header )
    {
        releaseResponse( session );
        return header;
    }

    @Override
    protected ChunkToken doResponseBody( AppTCPSession session, ChunkToken chunk )
    {
        return chunk;
    }

    @Override
    protected void doResponseBodyEnd( AppTCPSession session )
    {
    }

    @Override
    protected RequestLineToken doRequestLine( AppTCPSession session, RequestLineToken requestLine )
    {
        return requestLine;
    }

    @Override
    protected StatusLine doStatusLine( AppTCPSession session, StatusLine statusLine )
    {
        return statusLine;
    }

    // Generate a forbidden response for prefetch queries

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
