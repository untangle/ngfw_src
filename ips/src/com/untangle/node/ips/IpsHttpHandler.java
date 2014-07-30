/**
 * $Id$
 */
package com.untangle.node.ips;

import com.untangle.node.http.HttpEventHandler;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.StatusLine;
import com.untangle.node.token.ChunkToken;
import com.untangle.node.http.HeaderToken;
import com.untangle.uvm.vnet.NodeTCPSession;

class IpsHttpHandler extends HttpEventHandler {

    private IpsDetectionEngine engine;

    protected IpsHttpHandler( IpsNodeImpl node )
    {
        engine = node.getEngine();
    }

    protected RequestLineToken doRequestLine( NodeTCPSession session, RequestLineToken requestLine)
    {
        IpsSessionInfo info = engine.getSessionInfo( session );
        if (info != null) {
            // Null is no longer unusual, it happens whenever we've released the
            // session from the byte pipe.
            String path = requestLine.getRequestUri().normalize().getPath();
            info.setUriPath(path);
        }
        releaseRequest( session );
        return requestLine;
    }

    protected HeaderToken doRequestHeader( NodeTCPSession session, HeaderToken requestHeader )
    {
        return requestHeader;
    }

    protected void doRequestBodyEnd( NodeTCPSession session ) { }

    protected void doResponseBodyEnd( NodeTCPSession session ) { }

    protected ChunkToken doResponseBody( NodeTCPSession session, ChunkToken chunk )
    {
        return chunk;
    }

    protected HeaderToken doResponseHeader( NodeTCPSession session, HeaderToken header )
    {
        return header;
    }

    protected ChunkToken doRequestBody( NodeTCPSession session, ChunkToken chunk )
    {
        return chunk;
    }

    protected StatusLine doStatusLine( NodeTCPSession session, StatusLine statusLine )
    {
        releaseResponse( session );
        return statusLine;
    }
}
