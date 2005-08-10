/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.http;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.AbstractTokenHandler;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.Header;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenException;
import com.metavize.tran.token.TokenResult;

public abstract class HttpStateMachine extends AbstractTokenHandler
{
    protected enum ClientState {
        REQ_START_STATE,
        REQ_LINE_STATE,
        REQ_HEADER_STATE,
        REQ_BODY_STATE,
        REQ_BODY_END_STATE
    };

    protected enum ServerState {
        RESP_START_STATE,
        RESP_STATUS_LINE_STATE,
        RESP_HEADER_STATE,
        RESP_BODY_STATE,
        RESP_BODY_END_STATE
    };

    private ClientState clientState = ClientState.REQ_START_STATE;
    private ServerState serverState = ServerState.RESP_START_STATE;

    // constructors -----------------------------------------------------------

    protected HttpStateMachine(TCPSession session)
    {
        super(session);
    }

    // protected abstract methods ---------------------------------------------

    // XXX the default impls should pass through?

    protected abstract TokenResult doRequestLine(RequestLine rl)
        throws TokenException;
    protected abstract TokenResult doRequestHeader(Header h)
        throws TokenException;
    protected abstract TokenResult doRequestBody(Chunk c)
        throws TokenException;
    protected abstract TokenResult doRequestBodyEnd(EndMarker em)
        throws TokenException;

    protected abstract TokenResult doStatusLine(StatusLine sl)
        throws TokenException;
    protected abstract TokenResult doResponseHeader(Header h)
        throws TokenException;
    protected abstract TokenResult doResponseBody(Chunk c)
        throws TokenException;
    protected abstract TokenResult doResponseBodyEnd(EndMarker em)
        throws TokenException;

    // protected methods ------------------------------------------------------

    protected ClientState getClientState()
    {
        return clientState;
    }

    protected ServerState getServerState()
    {
        return serverState;

}
    // AbstractTokenHandler methods -------------------------------------------

    public TokenResult handleClientToken(Token token) throws TokenException
    {
        clientState = nextClientState(token);

        switch (clientState) {
        case REQ_LINE_STATE:
            RequestLine rl = (RequestLine)token;
            return doRequestLine(rl);

        case REQ_HEADER_STATE:
            Header h = (Header)token;
            return doRequestHeader(h);

        case REQ_BODY_STATE:
            Chunk c = (Chunk)token;
            return doRequestBody(c);

        case REQ_BODY_END_STATE:
            EndMarker em = (EndMarker)token;
            return doRequestBodyEnd(em);

        default:
            throw new IllegalStateException("Illegal state: " + clientState);
        }
    }

    public TokenResult handleServerToken(Token token) throws TokenException
    {
        serverState = nextServerState(token);

        switch (serverState) {
        case RESP_STATUS_LINE_STATE:
            StatusLine sl = (StatusLine)token;
            return doStatusLine(sl);

        case RESP_HEADER_STATE:
            Header h = (Header)token;
            return doResponseHeader(h);

        case RESP_BODY_STATE:
            Chunk c = (Chunk)token;
            return doResponseBody(c);

        case RESP_BODY_END_STATE:
            EndMarker em = (EndMarker)token;
            return doResponseBodyEnd(em);

        default:
            throw new IllegalStateException("Illegal state: " + clientState);
        }
    }

    // private methods --------------------------------------------------------

    private ClientState nextClientState(Object o)
    {
        switch (clientState) {
        case REQ_START_STATE:
            return ClientState.REQ_LINE_STATE;

        case REQ_LINE_STATE:
            return ClientState.REQ_HEADER_STATE;

        case REQ_HEADER_STATE:
            if (o instanceof Chunk) {
                return ClientState.REQ_BODY_STATE;
            } else {
                return ClientState.REQ_BODY_END_STATE;
            }

        case REQ_BODY_STATE:
            if (o instanceof Chunk) {
                return ClientState.REQ_BODY_STATE;
            } else {
                return ClientState.REQ_BODY_END_STATE;
            }

        case REQ_BODY_END_STATE:
            return ClientState.REQ_LINE_STATE;

        default:
            throw new IllegalStateException("Illegal state: " + clientState);
        }
    }


    private ServerState nextServerState(Object o)
    {
        switch (serverState) {
        case RESP_START_STATE:
            return ServerState.RESP_STATUS_LINE_STATE;

        case RESP_STATUS_LINE_STATE:
            return ServerState.RESP_HEADER_STATE;

        case RESP_HEADER_STATE:
            if (o instanceof Chunk) {
                return ServerState.RESP_BODY_STATE;
            } else {
                return ServerState.RESP_BODY_END_STATE;
            }

        case RESP_BODY_STATE:
            if (o instanceof Chunk) {
                return ServerState.RESP_BODY_STATE;
            } else {
                return ServerState.RESP_BODY_END_STATE;
            }

        case RESP_BODY_END_STATE:
            return ServerState.RESP_STATUS_LINE_STATE;

        default:
            throw new IllegalStateException("Illegal state: " + serverState);
        }
    }
}
