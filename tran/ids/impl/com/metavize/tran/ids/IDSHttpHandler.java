/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.ids;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.http.HttpStateMachine;
import com.metavize.tran.http.RequestLineToken;
import com.metavize.tran.http.StatusLine;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.Header;
import org.apache.log4j.Logger;

class IDSHttpHandler extends HttpStateMachine {

    private final Logger logger = Logger.getLogger(getClass());

    private IDSDetectionEngine engine;

    IDSHttpHandler(TCPSession session, IDSTransformImpl transform) {
        super(session);
        engine = transform.getEngine();
    }

    protected RequestLineToken doRequestLine(RequestLineToken requestLine) {
        IDSSessionInfo info = engine.getSessionInfo(getSession());
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
