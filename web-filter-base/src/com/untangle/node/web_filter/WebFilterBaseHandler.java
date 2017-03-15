/**
 * $Id$
 */
package com.untangle.node.web_filter;

import org.apache.log4j.Logger;

import com.untangle.node.http.HttpEventHandler;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.StatusLine;
import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.node.http.HeaderToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.AppTCPSession;

/**
 * Blocks HTTP traffic that is on an active block list.
 */
public class WebFilterBaseHandler extends HttpEventHandler
{
    protected final Logger logger = Logger.getLogger(getClass());

    protected final WebFilterBase node;

    // constructors -----------------------------------------------------------

    protected WebFilterBaseHandler( WebFilterBase node )
    {
        this.node = node;
    }

    // HttpEventHandler methods -----------------------------------------------

    @Override
    protected RequestLineToken doRequestLine( AppTCPSession session, RequestLineToken requestLine )
    {
        return requestLine;
    }

    @Override
    protected HeaderToken doRequestHeader( AppTCPSession sess, HeaderToken requestHeader )
    {
        node.incrementScanCount();

        String nonce = node.getDecisionEngine().checkRequest(sess, sess.getClientAddr(), 80, getRequestLine( sess ),requestHeader);

        if (logger.isDebugEnabled()) {
            logger.debug("in doRequestHeader(): " + requestHeader + "check request returns: " + nonce);
        }

        if ( nonce == null ) {
            node.incrementPassCount();
            
            releaseRequest( sess );
        } else {
            node.incrementBlockCount();

            String uri = getRequestLine( sess ).getRequestUri().toString();
            Token[] response = node.generateResponse( nonce, sess, uri, requestHeader );
            blockRequest( sess, response );
        }

        return requestHeader;
    }

    @Override
    protected ChunkToken doRequestBody( AppTCPSession session, ChunkToken c )
    {
        return c;
    }

    @Override
    protected void doRequestBodyEnd( AppTCPSession session )
    { }

    @Override
    protected StatusLine doStatusLine( AppTCPSession session, StatusLine statusLine )
    {
        return statusLine;
    }

    @Override
    protected HeaderToken doResponseHeader( AppTCPSession sess, HeaderToken responseHeader )
    {
        if ( getStatusLine( sess ).getStatusCode() == 100 ) {
            releaseResponse( sess );
        } else {
            String nonce = node.getDecisionEngine().checkResponse(sess, sess.getClientAddr(), getResponseRequest( sess ), responseHeader);
            
            if (logger.isDebugEnabled()) {
                logger.debug("in doResponseHeader: " + responseHeader + "checkResponse returns: " + nonce);
            }

            if ( nonce == null ) {
                node.incrementPassCount();

                releaseResponse( sess );
            } else {
                node.incrementBlockCount();

                Token[] response = node.generateResponse( nonce, sess );
                blockResponse( sess, response );
            }
        }

        return responseHeader;
    }

    @Override
    protected ChunkToken doResponseBody( AppTCPSession session, ChunkToken c )
    {
        return c;
    }

    @Override
    protected void doResponseBodyEnd( AppTCPSession session )
    { }
}
