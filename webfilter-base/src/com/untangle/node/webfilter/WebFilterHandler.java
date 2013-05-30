/**
 * $Id$
 */
package com.untangle.node.webfilter;

import org.apache.log4j.Logger;

import com.untangle.node.http.HttpStateMachine;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.StatusLine;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.Header;
import com.untangle.node.token.Token;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Blocks HTTP traffic that is on an active block list.
 */
public class WebFilterHandler extends HttpStateMachine
{
    protected final Logger logger = Logger.getLogger(getClass());

    protected final WebFilterBase node;

    // constructors -----------------------------------------------------------

    protected WebFilterHandler(NodeTCPSession session, WebFilterBase node)
    {
        super(session);

        this.node = node;
    }

    // HttpStateMachine methods -----------------------------------------------

    @Override
    protected RequestLineToken doRequestLine(RequestLineToken requestLine)
    {
        return requestLine;
    }

    @Override
    protected Header doRequestHeader(Header requestHeader)
    {
        node.incrementScanCount();

        NodeTCPSession sess = getSession();

        String nonce = node.getDecisionEngine().checkRequest(sess, sess.getClientAddr(), 80, getRequestLine(),requestHeader);

        if (logger.isDebugEnabled()) {
            logger.debug("in doRequestHeader(): " + requestHeader + "check request returns: " + nonce);
        }

        if (null == nonce) {
            releaseRequest();
        } else {
            node.incrementBlockCount();
            String uri = getRequestLine().getRequestUri().toString();
            Token[] response = node.generateResponse(nonce, sess, uri, requestHeader );

            blockRequest(response);
        }

        return requestHeader;
    }

    @Override
    protected Chunk doRequestBody(Chunk c)
    {
        return c;
    }

    @Override
    protected void doRequestBodyEnd() { }

    @Override
    protected StatusLine doStatusLine(StatusLine statusLine)
    {
        return statusLine;
    }

    @Override
    protected Header doResponseHeader(Header responseHeader)
    {
        if (100 == getStatusLine().getStatusCode()) {
            releaseResponse();
        } else {
            NodeTCPSession sess = getSession();

            String nonce = node.getDecisionEngine().checkResponse(sess, sess.getClientAddr(), getResponseRequest(),responseHeader);
            
            if (logger.isDebugEnabled()) {
                logger.debug("in doResponseHeader: " + responseHeader + "checkResponse returns: " + nonce);
            }

            if (null == nonce) {
                node.incrementPassCount();

                releaseResponse();
            } else {
                node.incrementBlockCount();
                Token[] response = node.generateResponse(nonce, sess);
                blockResponse(response);
            }
        }

        return responseHeader;
    }

    @Override
    protected Chunk doResponseBody(Chunk c)
    {
        return c;
    }

    @Override
    protected void doResponseBodyEnd() { }
}
