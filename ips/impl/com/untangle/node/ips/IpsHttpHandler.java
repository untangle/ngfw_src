/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.ips;

import com.untangle.node.http.HttpStateMachine;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.StatusLine;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.Header;
import com.untangle.uvm.vnet.TCPSession;

class IpsHttpHandler extends HttpStateMachine {

    private IpsDetectionEngine engine;

    IpsHttpHandler(TCPSession session, IpsNodeImpl node) {
        super(session);
        engine = node.getEngine();
    }

    protected RequestLineToken doRequestLine(RequestLineToken requestLine) {
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

    protected Header doRequestHeader(Header requestHeader) {
        return requestHeader;
    }

    protected void doRequestBodyEnd() { }

    protected void doResponseBodyEnd() { }

    protected Chunk doResponseBody(Chunk chunk) {
        return chunk;
    }

    protected Header doResponseHeader(Header header) {
        return header;
    }

    protected Chunk doRequestBody(Chunk chunk) {
        return chunk;
    }

    protected StatusLine doStatusLine(StatusLine statusLine) {
        releaseResponse();
        return statusLine;
    }
}
