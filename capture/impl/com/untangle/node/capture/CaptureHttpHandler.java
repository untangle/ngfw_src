/**
 * $Id: CaptureHttpHandler.java 31921 2012-05-12 02:44:47Z mahotz $
 */

package com.untangle.node.capture;

import java.net.InetAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.untangle.node.http.HttpStateMachine;
import com.untangle.node.http.HttpMethod;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.RequestLine;
import com.untangle.node.http.StatusLine;
import com.untangle.node.token.TokenException;
import com.untangle.node.token.EndMarker;
import com.untangle.node.token.Header;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.Token;
import com.untangle.uvm.vnet.NodeTCPSession;

class CaptureHttpHandler extends HttpStateMachine
{
    private final Logger logger = Logger.getLogger(getClass());
    private final CaptureNodeImpl node;

    // constructors -----------------------------------------------------------

    CaptureHttpHandler(NodeTCPSession session, CaptureNodeImpl node)
    {
        super(session);
        this.node = node;
    }

    // HttpStateMachine methods -----------------------------------------------

    @Override
    protected Header doRequestHeader(Header requestHeader)
    {
        Token[] response = null;
        NodeTCPSession session = getSession();

        // first check to see if the user is already authenticated
        if (node.isClientAuthenticated(session.getClientAddr()) == true)
        {
            node.incrementBlinger(CaptureNode.BlingerType.SESSALLOW,1);
            releaseRequest();
            return(requestHeader);
        }

        // not authenticated so check both of the pass lists
        PassedAddress passed = node.isSessionAllowed(session.getClientAddr(),session.getServerAddr());

            if (passed != null)
            {
                if (passed.getLog() == true)
                {
                CaptureRuleEvent logevt = new CaptureRuleEvent(session.sessionEvent(), false);
                node.logEvent(logevt);
                }

            node.incrementBlinger(CaptureNode.BlingerType.SESSALLOW,1);
            releaseRequest();
            return(requestHeader);
            }

        // not authenticated and no pass list match so check the rules
        CaptureRule rule = node.checkCaptureRules(session);

        // by default we allow traffic so if there is no rule pass the traffic
        if (rule == null)
        {
            node.incrementBlinger(CaptureNode.BlingerType.SESSALLOW,1);
            releaseRequest();
            return(requestHeader);
        }

        // if we found a pass rule then log and let the traffic pass
        if (rule.getCapture() == false)
        {
            CaptureRuleEvent logevt = new CaptureRuleEvent(session.sessionEvent(), rule);
            node.logEvent(logevt);

            node.incrementBlinger(CaptureNode.BlingerType.SESSALLOW,1);
            releaseRequest();
            return(requestHeader);
        }

        String method = getRequestLine().getMethod().toString();
        String uri = getRequestLine().getRequestUri().toString();

        // look for a host in the request line
        String host = getRequestLine().getRequestUri().getHost();

        // if not found there look in the request header
        if (host == null) host = requestHeader.getValue("Host");

        // if still not found then just use the IP address of the server
        if (host == null) host = getSession().getServerAddr().getHostAddress().toString();

        host = host.toLowerCase();

        // look for prefetch shenaniganery
        String prefetch = requestHeader.getValue("X-moz");

            // found a prefetch request so return a special error response
            if ((prefetch != null) && (prefetch.contains("prefetch") == true))
            {
                response = generatePrefetchResponse();
            }

            // not a prefetch so generate the captive portal redirect
            else
            {
                CaptureBlockDetails details = new CaptureBlockDetails(host, uri, method);
                response = node.generateResponse(details, session);
            }

        CaptureRuleEvent logevt = new CaptureRuleEvent(session.sessionEvent(), rule);
        node.logEvent(logevt);
        node.incrementBlinger(CaptureNode.BlingerType.SESSBLOCK,1);
        blockRequest(response);
        return requestHeader;
    }

    @Override
    protected Chunk doRequestBody(Chunk chunk) throws TokenException
    {
        return chunk;
    }

    @Override
    protected void doRequestBodyEnd() throws TokenException
    {
    }

    @Override
    protected Header doResponseHeader(Header header)
    {
        releaseResponse();
        return header;
    }

    @Override
    protected Chunk doResponseBody( Chunk chunk ) throws TokenException
    {
        return chunk;
    }

    @Override
    protected void doResponseBodyEnd( ) throws TokenException
    {
    }

    @Override
    protected RequestLineToken doRequestLine(RequestLineToken requestLine) throws TokenException
    {
        return requestLine;
    }

    @Override
    protected StatusLine doStatusLine(StatusLine statusLine) throws TokenException
    {
        return statusLine;
    }

///// Generate a forbidden response for prefetch queries

    private Token[] generatePrefetchResponse()
    {
        Token response[] = new Token[4];

        StatusLine sl = new StatusLine("HTTP/1.1", 503, "Service Unavailable");
        response[0] = sl;

        Header head = new Header();
        head.addField("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
        head.addField("Pragma", "no-cache");
        head.addField("Expires", "Mon, 10 Jan 2000 00:00:00 GMT");
        head.addField("Content-Length", "0");
        head.addField("Connection", "Close");
        response[1] = head;

        response[2] = Chunk.EMPTY;
        response[3] = EndMarker.MARKER;
        return(response);
    }
}
