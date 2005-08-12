package com.metavize.tran.ids;


import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;

public class IDSHttpFactory implements TokenHandlerFactory {
	private final IDSTransformImpl transform;
	
	IDSHttpFactory(IDSTransformImpl transform) {
		this.transform = transform;
	}
	
	public TokenHandler tokenHandler(TCPSession session) {
		return new IDSHttpHandler(session, transform);
	}
}

