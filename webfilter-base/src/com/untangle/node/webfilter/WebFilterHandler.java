/**
 * $Id$
 */
package com.untangle.node.webfilter;

import org.apache.log4j.Logger;

import com.untangle.node.http.HttpStateMachine;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.StatusLine;
import com.untangle.node.token.ChunkToken;
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

    protected WebFilterHandler( WebFilterBase node )
    {
        this.node = node;
    }

    // HttpStateMachine methods -----------------------------------------------

    @Override
    protected RequestLineToken doRequestLine( NodeTCPSession session, RequestLineToken requestLine )
    {
        return requestLine;
    }

    @Override
    protected Header doRequestHeader( NodeTCPSession sess, Header requestHeader )
    {
        node.incrementScanCount();

        String nonce = node.getDecisionEngine().checkRequest(sess, sess.getClientAddr(), 80, getRequestLine( sess ),requestHeader);

        if (logger.isDebugEnabled()) {
            logger.debug("in doRequestHeader(): " + requestHeader + "check request returns: " + nonce);
        }

        if (null == nonce) {
            releaseRequest( sess );
        } else {
            node.incrementBlockCount();
            String uri = getRequestLine( sess ).getRequestUri().toString();
            Token[] response = node.generateResponse(nonce, sess, uri, requestHeader );

            blockRequest( sess, response );
        }

        return requestHeader;
    }

    @Override
    protected ChunkToken doRequestBody( NodeTCPSession session, ChunkToken c )
    {
        return c;
    }

    @Override
    protected void doRequestBodyEnd( NodeTCPSession session )
    { }

    @Override
    protected StatusLine doStatusLine( NodeTCPSession session, StatusLine statusLine )
    {
        return statusLine;
    }

    @Override
    protected Header doResponseHeader( NodeTCPSession sess, Header responseHeader )
    {
        if ( getStatusLine( sess ).getStatusCode() == 100 ) {
            releaseResponse( sess );
        } else {
            String nonce = node.getDecisionEngine().checkResponse(sess, sess.getClientAddr(), getResponseRequest( sess ), responseHeader);
            
            if (logger.isDebugEnabled()) {
                logger.debug("in doResponseHeader: " + responseHeader + "checkResponse returns: " + nonce);
            }

            if (null == nonce) {
                node.incrementPassCount();

                releaseResponse( sess );
            } else {
                node.incrementBlockCount();
                Token[] response = node.generateResponse(nonce, sess);
                blockResponse( sess, response );
            }
        }

        return responseHeader;
    }

    @Override
    protected ChunkToken doResponseBody( NodeTCPSession session, ChunkToken c )
    {
        return c;
    }

    @Override
    protected void doResponseBodyEnd( NodeTCPSession session )
    { }
}
