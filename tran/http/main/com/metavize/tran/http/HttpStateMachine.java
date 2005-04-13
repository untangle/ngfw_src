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
import com.metavize.tran.token.TokenResult;

public abstract class HttpStateMachine extends AbstractTokenHandler
{
    private static final int REQ_START_STATE = 0;
    private static final int REQ_LINE_STATE = 1;
    private static final int REQ_HEADER_STATE = 2;
    private static final int REQ_BODY_STATE = 3;
    private static final int REQ_BODY_END_STATE = 4;

    private static final int RESP_START_STATE = 5;
    private static final int RESP_STATUS_LINE_STATE = 6;
    private static final int RESP_HEADER_STATE = 7;
    private static final int RESP_BODY_STATE = 8;
    private static final int RESP_BODY_END_STATE = 9;

    private int clientState = REQ_START_STATE;
    private int serverState = RESP_START_STATE;

    // constructors -----------------------------------------------------------

    protected HttpStateMachine(TCPSession session)
    {
        super(session);
    }

    // protected abstract -----------------------------------------------------

    // XXX the default impls should pass through?

    protected abstract TokenResult doRequestLine(RequestLine rl);
    protected abstract TokenResult doRequestHeader(Header h);
    protected abstract TokenResult doRequestBody(Chunk c);
    protected abstract TokenResult doRequestBodyEnd(EndMarker em);

    protected abstract TokenResult doStatusLine(StatusLine sl);
    protected abstract TokenResult doResponseHeader(Header h);
    protected abstract TokenResult doResponseBody(Chunk c);
    protected abstract TokenResult doResponseBodyEnd(EndMarker em);

    // AbstractTokenHandler methods -------------------------------------------

    public TokenResult handleClientToken(Token token)
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

    public TokenResult handleServerToken(Token token)
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


    private int nextClientState(Object o)
    {
        switch (clientState) {
        case REQ_START_STATE:
            return REQ_LINE_STATE;

        case REQ_LINE_STATE:
            return REQ_HEADER_STATE;

        case REQ_HEADER_STATE:
            if (o instanceof Chunk) {
                return REQ_BODY_STATE;
            } else {
                return REQ_BODY_END_STATE;
            }

        case REQ_BODY_STATE:
            if (o instanceof Chunk) {
                return REQ_BODY_STATE;
            } else {
                return REQ_BODY_END_STATE;
            }

        case REQ_BODY_END_STATE:
            return REQ_LINE_STATE;

        default:
            throw new IllegalStateException("Illegal state: " + clientState);
        }
    }


    private int nextServerState(Object o)
    {
        switch (serverState) {
        case RESP_START_STATE:
            return RESP_STATUS_LINE_STATE;

        case RESP_STATUS_LINE_STATE:
            return RESP_HEADER_STATE;

        case RESP_HEADER_STATE:
            if (o instanceof Chunk) {
                return RESP_BODY_STATE;
            } else {
                return RESP_BODY_END_STATE;
            }

        case RESP_BODY_STATE:
            if (o instanceof Chunk) {
                return RESP_BODY_STATE;
            } else {
                return RESP_BODY_END_STATE;
            }

        case RESP_BODY_END_STATE:
            return RESP_STATUS_LINE_STATE;

        default:
            throw new IllegalStateException("Illegal state: " + serverState);
        }
    }
}
