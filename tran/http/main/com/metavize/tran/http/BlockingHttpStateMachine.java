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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.AbstractTokenHandler;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.Header;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenException;
import com.metavize.tran.token.TokenResult;

// XXX will probably roll this into a more general system
public abstract class BlockingHttpStateMachine extends AbstractTokenHandler
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

    private enum Mode {
        QUEUEING,
        RELEASED,
        BLOCKED
    };

    private final List<RequestLine> requests = new LinkedList<RequestLine>();

    private final List<Token[]> outstandingResponses = new LinkedList<Token[]>();

    private final List<Token> requestQueue = new ArrayList<Token>();
    private final List<Token> responseQueue = new ArrayList<Token>();

    private ClientState clientState = ClientState.REQ_START_STATE;
    private ServerState serverState = ServerState.RESP_START_STATE;

    private Mode requestMode = Mode.QUEUEING;
    private Mode responseMode = Mode.QUEUEING;

    private RequestLine requestLine;
    private RequestLine responseRequest;
    private StatusLine statusLine;

    private Token[] requestResponse = null;
    private Token[] responseResponse = null;

    private boolean requestPersistent;
    private boolean responsePersistent;

    // constructors -----------------------------------------------------------

    protected BlockingHttpStateMachine(TCPSession session)
    {
        super(session);
    }

    // protected abstract methods ---------------------------------------------

    // XXX the default impls should pass through?

    protected abstract RequestLine doRequestLine(RequestLine rl)
        throws TokenException;
    protected abstract Header doRequestHeader(Header h)
        throws TokenException;
    protected abstract Chunk doRequestBody(Chunk c)
        throws TokenException;
    protected abstract void doRequestBodyEnd()
        throws TokenException;

    protected abstract StatusLine doStatusLine(StatusLine sl)
        throws TokenException;
    protected abstract Header doResponseHeader(Header h)
        throws TokenException;
    protected abstract Chunk doResponseBody(Chunk c)
        throws TokenException;
    protected abstract void doResponseBodyEnd()
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

    protected RequestLine getRequestLine()
    {
        return requestLine;
    }

    protected RequestLine getResponseRequest()
    {
        return responseRequest;
    }

    protected StatusLine getStatusLine()
    {
        return statusLine;
    }

    protected boolean isRequestPersistent()
    {
        return requestPersistent;
    }

    protected boolean isResponsePersistent()
    {
        return responsePersistent;
    }

    protected void releaseRequest()
    {
        if (Mode.QUEUEING != requestMode) {
            throw new IllegalStateException("releaseRequest in: " + requestMode);
        }

        requestMode = Mode.RELEASED;
    }

    protected void blockRequest(Token[] response)
    {
        if (Mode.QUEUEING != requestMode) {
            throw new IllegalStateException("blockRequest in: " + requestMode);
        }

        requestMode = Mode.BLOCKED;
        requestResponse = response;
    }

    protected void releaseResponse()
    {
        if (Mode.QUEUEING != responseMode) {
            throw new IllegalStateException("releaseResponse in: " + responseMode);
        }

        responseMode = Mode.RELEASED;
    }

    protected void blockResponse(Token[] response)
    {
        if (Mode.QUEUEING != responseMode) {
            throw new IllegalStateException("releaseResponse in: " + responseMode);
        }

        responseMode = Mode.BLOCKED;
        responseResponse = response;
    }


    // AbstractTokenHandler methods -------------------------------------------

    public TokenResult handleClientToken(Token token) throws TokenException
    {
        clientState = nextClientState(token);

        switch (clientState) {
        case REQ_LINE_STATE:
            requestMode = Mode.QUEUEING;
            requestLine = (RequestLine)token;
            requests.add(requestLine);
            requestLine = doRequestLine(requestLine);

            switch (requestMode) {
            case QUEUEING:
                requestQueue.add(requestLine);
                return TokenResult.NONE;

            case RELEASED:
                assert 0 == requestQueue.size();

                return new TokenResult(null, new Token[] { requestLine } );

            case BLOCKED:
                assert 0 == requestQueue.size();

                if (1 == requests.size()) {
                    requests.remove(requests.size() - 1);
                    TokenResult tr = new TokenResult(requestResponse, null);
                    requestResponse = null;
                    return tr;
                } else {
                    outstandingResponses.add(requestResponse);
                    requestResponse = null;
                    return TokenResult.NONE;
                }

            default:
                throw new IllegalStateException("programmer malfunction");
            }

        case REQ_HEADER_STATE:
            if (Mode.BLOCKED != requestMode) {
                Header h = (Header)token;
                requestPersistent = isPersistent(h);
                h = doRequestHeader(h);

                switch (requestMode) {
                case QUEUEING:
                    requestQueue.add(h);
                    return TokenResult.NONE;

                case RELEASED:
                    requestQueue.add(h);
                    Token[] toks = new Token[requestQueue.size()];
                    toks = requestQueue.toArray(toks);
                    requestQueue.clear();
                    return new TokenResult(null, toks);

                case BLOCKED:
                    requestQueue.clear();

                    if (1 == requests.size()) {
                        requests.remove(requests.size() - 1);
                        TokenResult tr = new TokenResult(requestResponse, null);
                        requestResponse = null;
                        return tr;
                    } else {
                        outstandingResponses.add(requestResponse);
                        requestResponse = null;
                        return TokenResult.NONE;
                    }

                default:
                    throw new IllegalStateException("programmer malfunction");
                }
            } else {
                return TokenResult.NONE;
            }

        case REQ_BODY_STATE:
            if (Mode.BLOCKED != requestMode) {
                Chunk c = (Chunk)token;
                c = doRequestBody(c);

                switch (requestMode) {
                case QUEUEING:
                    requestQueue.add(c);
                    return TokenResult.NONE;

                case RELEASED:
                    requestQueue.add(c);
                    Token[] toks = new Token[requestQueue.size()];
                    toks = requestQueue.toArray(toks);
                    requestQueue.clear();
                    return new TokenResult(null, toks);

                case BLOCKED:
                    requestQueue.clear();

                    if (1 == requests.size()) {
                        requests.remove(requests.size() - 1);
                        TokenResult tr = new TokenResult(requestResponse, null);
                        requestResponse = null;
                        return tr;
                    } else {
                        outstandingResponses.add(requestResponse);
                        requestResponse = null;
                        return TokenResult.NONE;
                    }

                default:
                    throw new IllegalStateException("programmer malfunction");
                }

            } else {
                return TokenResult.NONE;
            }

        case REQ_BODY_END_STATE:
            if (Mode.BLOCKED != requestMode) {
                EndMarker em = (EndMarker)token;
                doRequestBodyEnd();

                switch (requestMode) {
                case QUEUEING:
                    throw new IllegalStateException
                        ("queueing request after EndMarker");

                case RELEASED:
                    doRequestBodyEnd();

                    requestQueue.add(EndMarker.MARKER);
                    Token[] toks = new Token[requestQueue.size()];
                    toks = requestQueue.toArray(toks);
                    requestQueue.clear();
                    return new TokenResult(null, toks);

                case BLOCKED:
                    requestQueue.clear();

                    if (1 == requests.size()) {
                        requests.remove(requests.size() - 1);
                        TokenResult tr = new TokenResult(requestResponse, null);
                        requestResponse = null;
                        return tr;
                    } else {
                        outstandingResponses.add(requestResponse);
                        requestResponse = null;
                        return TokenResult.NONE;
                    }

                default:
                    throw new IllegalStateException("programmer malfunction");
                }
            } else {
                return TokenResult.NONE;
            }

        default:
            throw new IllegalStateException("programmer malfunction");
        }
    }

    public TokenResult handleServerToken(Token token) throws TokenException
    {
        serverState = nextServerState(token);

        switch (serverState) {
        case RESP_STATUS_LINE_STATE:
            responseMode = Mode.QUEUEING;

            statusLine = (StatusLine)token;
            if (100 != statusLine.getStatusCode()) {
                responseRequest = (RequestLine)requests.remove(0);
            }

            statusLine = doStatusLine(statusLine);

            switch (responseMode) {
            case QUEUEING:
                responseQueue.add(statusLine);
                return TokenResult.NONE;

            case RELEASED:
                assert 0 == responseQueue.size();

                return new TokenResult(new Token[] { statusLine }, null);

            case BLOCKED:
                assert 0 == responseQueue.size();

                TokenResult tr = new TokenResult(responseResponse, null);
                responseResponse = null;
                return tr;

            default:
                throw new IllegalStateException("programmer malfunction");
            }

        case RESP_HEADER_STATE:
            if (Mode.BLOCKED != responseMode) {
                Header h = (Header)token;
                responsePersistent = isPersistent(h);
                h = doResponseHeader(h);

                switch (responseMode) {
                case QUEUEING:
                    responseQueue.add(h);
                    return TokenResult.NONE;

                case RELEASED:
                    responseQueue.add(h);
                    Token[] toks = new Token[responseQueue.size()];
                    toks = responseQueue.toArray(toks);
                    responseQueue.clear();
                    return new TokenResult(toks, null);

                case BLOCKED:
                    responseQueue.clear();
                    TokenResult tr = new TokenResult(responseResponse, null);
                    responseResponse = null;
                    return tr;

                default:
                    throw new IllegalStateException("programmer malfunction");
                }
            } else {
                return TokenResult.NONE;
            }

        case RESP_BODY_STATE:
            if (Mode.BLOCKED != responseMode) {
                Chunk c = (Chunk)token;
                c = doResponseBody(c);

                switch (responseMode) {
                case QUEUEING:
                    responseQueue.add(c);
                    return TokenResult.NONE;

                case RELEASED:
                    responseQueue.add(c);
                    Token[] toks = new Token[responseQueue.size()];
                    toks = responseQueue.toArray(toks);
                    responseQueue.clear();
                    return new TokenResult(toks, null);

                case BLOCKED:
                    responseQueue.clear();
                    TokenResult tr = new TokenResult(responseResponse, null);
                    responseResponse = null;
                    return tr;

                default:
                    throw new IllegalStateException("programmer malfunction");
                }
            } else {
                return TokenResult.NONE;
            }

        case RESP_BODY_END_STATE:
            if (Mode.BLOCKED != responseMode) {
                EndMarker em = (EndMarker)token;
                doResponseBodyEnd();

                switch (responseMode) {
                case QUEUEING:
                    throw new IllegalStateException("queueing after EndMarker");

                case RELEASED:
                    responseQueue.add(em);
                    Token[] toks = new Token[responseQueue.size()];
                    toks = responseQueue.toArray(toks);
                    responseQueue.clear();
                    return new TokenResult(toks, null);

                case BLOCKED:
                    responseQueue.clear();
                    TokenResult tr = new TokenResult(responseResponse, null);
                    responseResponse = null;
                    return tr;

                default:
                    throw new IllegalStateException("programmer malfunction");
                }

            } else {
                return TokenResult.NONE;
            }

        default:
            throw new IllegalStateException("programmer malfunction");
        }
    }

    @Override
    public TokenResult releaseFlush()
    {
        // XXX we do not even attempt to deal with outstanding block pages
        Token[] req = new Token[requestQueue.size()];
        req = requestQueue.toArray(req);
        Token[] resp = new Token[responseQueue.size()];
        resp = responseQueue.toArray(resp);
        return new TokenResult(resp, req);
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

    private boolean isPersistent(Header header)
    {
        String con = header.getValue("connection");
        return null == con ? false : con.equalsIgnoreCase("keep-alive");
    }
}
