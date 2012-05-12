/**
 * $Id$
 */
package com.untangle.node.ips;

import com.untangle.node.http.HttpStateMachine;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.StatusLine;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.Header;
import com.untangle.uvm.vnet.NodeTCPSession;

class IpsHttpHandler extends HttpStateMachine {

    private IpsDetectionEngine engine;

    IpsHttpHandler(NodeTCPSession session, IpsNodeImpl node)
    {
        super(session);
        engine = node.getEngine();
    }

    protected RequestLineToken doRequestLine(RequestLineToken requestLine)
    {
        IpsSessionInfo info = engine.getSessionInfo(getSession());
        if (info != null) {
            // Null is no longer unusual, it happens whenever we've released the
            // session from the byte pipe.
            String path = requestLine.getRequestUri().normalize().getPath();
            info.setUriPath(path);
        }
        releaseRequest();
        return requestLine;
    }

    protected Header doRequestHeader(Header requestHeader)
    {
        return requestHeader;
    }

    protected void doRequestBodyEnd() { }

    protected void doResponseBodyEnd() { }

    protected Chunk doResponseBody(Chunk chunk)
    {
        return chunk;
    }

    protected Header doResponseHeader(Header header)
    {
        return header;
    }

    protected Chunk doRequestBody(Chunk chunk)
    {
        return chunk;
    }

    protected StatusLine doStatusLine(StatusLine statusLine)
    {
        releaseResponse();
        return statusLine;
    }
}
