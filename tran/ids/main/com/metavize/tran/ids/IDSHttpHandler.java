package com.metavize.tran.ids;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.http.HttpStateMachine;
import com.metavize.tran.http.RequestLine;
import com.metavize.tran.http.StatusLine;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.Header;

class IDSHttpHandler extends HttpStateMachine {

    private IDSTransformImpl transform; 
	private IDSSessionInfo info;

    IDSHttpHandler(TCPSession session, IDSTransformImpl transform) {
        super(session);
		this.transform = transform;
        transform.getEngine().mapSessionInfo(session.id(),new IDSSessionInfo());
    }

    protected RequestLine doRequestLine(RequestLine requestLine) {
        String path = requestLine.getRequestUri().getPath();
        IDSSessionInfo info = transform.getEngine().getSessionInfo(super.getSession().id());
        info.setUriPath(path);
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
