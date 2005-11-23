package com.metavize.tran.ids;

import org.apache.log4j.Logger;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.http.HttpStateMachine;
import com.metavize.tran.http.RequestLineToken;
import com.metavize.tran.http.StatusLine;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.Header;

class IDSHttpHandler extends HttpStateMachine {

    private static final Logger logger = Logger.getLogger(IDSHttpHandler.class);

    private IDSDetectionEngine engine;

    IDSHttpHandler(TCPSession session, IDSTransformImpl transform) {
        super(session);
        engine = transform.getEngine();
    }

    protected RequestLineToken doRequestLine(RequestLineToken requestLine) {
        IDSSessionInfo info = engine.getSessionInfo(getSession());
        if (info == null) {
            logger.warn("No session info at doRequestLine time");
        } else {
            String path = requestLine.getRequestUri().getPath();
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
