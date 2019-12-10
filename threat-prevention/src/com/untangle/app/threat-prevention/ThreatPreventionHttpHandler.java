/**
 * $Id: WebFilterHandler.java 42622 2016-03-08 23:00:30Z dmorris $
 */

package com.untangle.app.threat_prevention;

import java.net.URI;

import com.untangle.app.http.HttpEventHandler;
import com.untangle.app.http.RequestLineToken;
import com.untangle.app.http.HeaderToken;
import com.untangle.app.http.StatusLine;
import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.Token;
// import com.untangle.app.web_filter.WebFilterBase;
// import com.untangle.app.web_filter.WebFilterHandler;
import com.untangle.uvm.vnet.AppTCPSession;

import org.apache.log4j.Logger;


/**
 * Blocks HTTP traffic that is on an active block list.
 */
public class ThreatPreventionHttpHandler extends HttpEventHandler
{
    private final Logger logger = Logger.getLogger(ThreatPreventionHttpHandler.class);

    protected final ThreatPreventionApp app;

    /**
     * Constructor
     * 
     * @param app
     *        The web filter base application
     */
    public ThreatPreventionHttpHandler(ThreatPreventionApp app)
    {
        this.app = app;
    }

    /**
     * Handle the request line
     * 
     * @param session
     *        The session
     * @param requestLine
     *        The request line
     * @return The request line
     */
    @Override
    protected RequestLineToken doRequestLine(AppTCPSession session, RequestLineToken requestLine)
    {
        return requestLine;
    }

    /**
     * Handle the request header
     * 
     * @param session
     *        The session
     * @param requestHeader
     *        The request header
     * @return The request header
     */
    @Override
    protected HeaderToken doRequestHeader(AppTCPSession session, HeaderToken requestHeader)
    {
        // app.incrementScanCount();

        ThreatPreventionBlockDetails redirectDetails = app.getDecisionEngine().checkRequest(session, session.getClientAddr(), session.getServerPort(), getRequestLine(session), requestHeader);
        if (logger.isDebugEnabled()) {
            logger.debug("in doRequestHeader(): " + requestHeader + "check request returns: " + redirectDetails);
        }

        if (redirectDetails == null) {
            String host = requestHeader.getValue("Host");
            URI uri = getRequestLine(session).getRequestUri();

            releaseRequest(session);
        } else {
            app.incrementBlockCount();
            String uri = getRequestLine(session).getRequestUri().toString();
            Token[] response = app.generateHttpResponse( redirectDetails, session, uri, requestHeader);

            blockRequest(session, response);
        }

        return requestHeader;
    }
    /**
     * Handle the request body
     * 
     * @param session
     *        The session
     * @param chunk
     *        The chunk
     * @return The chunk
     */
    @Override
    protected ChunkToken doRequestBody(AppTCPSession session, ChunkToken chunk)
    {
        return chunk;
    }

    /**
     * Handle the reqest body end
     * 
     * @param session
     *        The session
     */
    @Override
    protected void doRequestBodyEnd(AppTCPSession session)
    {
    }

    /**
     * Handle the status line
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
     * Handle the response header
     * 
     * @param sess
     *        The session
     * @param responseHeader
     *        The response header
     * @return The response header
     */
    @Override
    protected HeaderToken doResponseHeader(AppTCPSession sess, HeaderToken responseHeader)
    {
        releaseResponse(sess);
        return responseHeader;
    }

    /**
     * Handle the response body
     * 
     * @param session
     *        The session
     * @param chunk
     *        The chunk
     * @return The chunk
     */
    @Override
    protected ChunkToken doResponseBody(AppTCPSession session, ChunkToken chunk)
    {
        return chunk;
    }

    /**
     * Handle the response body end
     * 
     * @param session
     *        The session
     */
    @Override
    protected void doResponseBodyEnd(AppTCPSession session)
    {
    }
}
