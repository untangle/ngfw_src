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

	private IDSTransform transform; //Do i need this?
	
	IDSHttpHandler(TCPSession session, IDSTransformImpl transform) {
		super(session);
		System.out.println("AAAAAAAAA");
		this.transform = transform;
	}
	
	protected TokenResult doRequestLine(RequestLine requestLine) {
		String path = requestLine.getRequestUri().getPath();
		System.out.println(" AIEEEEEEEEEEEEEE ::::: " + path);
		return new TokenResult(null, new Token[] { requestLine });
	}

	protected TokenResult doRequestHeader(Header requestHeader) {
		System.out.println("Ehh");
		return TokenResult.NONE;
	}

	protected TokenResult doRequestBodyEnd(EndMarker endMarker) {
		System.out.println("Ehh");
		return new TokenResult(null, new Token[] { endMarker });
	}

	protected TokenResult doResponseBodyEnd(EndMarker endMarker) {
		System.out.println("Ehh");
		return new TokenResult(new Token[] { endMarker }, null);
	}

	protected TokenResult doResponseBody(Chunk chunk) {
		System.out.println("Ehh");
		return new TokenResult(new Token[] { chunk }, null);
	}

	protected TokenResult doResponseHeader(Header header) {
		System.out.println("Ehh");
		return new TokenResult(new Token[] { header }, null);
	}
	
	protected TokenResult doRequestBody(Chunk chunk) {
		System.out.println("Ehh");
		return new TokenResult(null, new Token[] { chunk });
	}
	
	protected TokenResult doStatusLine(StatusLine statusLine) {
		System.out.println("Ehh");
		return new TokenResult(new Token[] { statusLine }, null);
	}
}
