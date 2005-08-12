package com.metavize.tran.ids;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.http.HttpStateMachine;
import com.metavize.tran.http.RequestLine;
import com.metavize.tran.http.StatusLine;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.Header;
import com.metavize.tran.token.TokenResult;
import com.metavize.tran.token.EndMarker;

class IDSHttpHandler extends HttpStateMachine {

	//private IDSTransform transform; //Do i need this?
	private IDSSessionInfo info;
	
	IDSHttpHandler(TCPSession session, IDSTransformImpl transform) {
		super(session);
		IDSDetectionEngine.instance().mapSessionInfo(session.id(),new IDSSessionInfo());
	}
	
	protected TokenResult doRequestLine(RequestLine requestLine) {
		String path = requestLine.getRequestUri().getPath();
		IDSSessionInfo info = IDSDetectionEngine.instance().getSessionInfo(super.getSession().id());
		info.setUriPath(path);
		return new TokenResult(null, new Token[] { requestLine });
	}

	protected TokenResult doRequestHeader(Header requestHeader) {
		return new TokenResult(null, new Token[] { requestHeader });
	}

	protected TokenResult doRequestBodyEnd(EndMarker endMarker) {		
		return new TokenResult(null, new Token[] { endMarker });
	}

	protected TokenResult doResponseBodyEnd(EndMarker endMarker) {
		return new TokenResult(new Token[] { endMarker }, null);
	}

	protected TokenResult doResponseBody(Chunk chunk) {
		return new TokenResult(new Token[] { chunk }, null);
	}

	protected TokenResult doResponseHeader(Header header) {
		return new TokenResult(new Token[] { header }, null);
	}
	
	protected TokenResult doRequestBody(Chunk chunk) {
		return new TokenResult(null, new Token[] { chunk });
	}
	
	protected TokenResult doStatusLine(StatusLine statusLine) {
		return new TokenResult(new Token[] { statusLine }, null);
	}
}
