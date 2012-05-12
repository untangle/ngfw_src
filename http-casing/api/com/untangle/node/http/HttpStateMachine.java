/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.node.token.AbstractTokenHandler;
import com.untangle.node.token.ArrayTokenStreamer;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.EndMarker;
import com.untangle.node.token.Header;
import com.untangle.node.token.SeriesTokenStreamer;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenException;
import com.untangle.node.token.TokenResult;
import com.untangle.node.token.TokenStreamer;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Adapts a stream of HTTP tokens to methods relating to the protocol
 * state.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class HttpStateMachine extends AbstractTokenHandler
{
    private final Logger logger = Logger.getLogger(getClass());

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

    protected enum Mode {
        QUEUEING,
        RELEASED,
        BLOCKED
    };

    private enum Task {
        NONE,
        REQUEST,
        RESPONSE
    }

    private final List<RequestLineToken> requests = new LinkedList<RequestLineToken>();

    private final List<Token[]> outstandingResponses = new LinkedList<Token[]>();

    private final List<Token> requestQueue = new ArrayList<Token>();
    private final List<Token> responseQueue = new ArrayList<Token>();
    private final Map<RequestLineToken, String> hosts = new HashMap<RequestLineToken, String>();

    private ClientState clientState = ClientState.REQ_START_STATE;
    private ServerState serverState = ServerState.RESP_START_STATE;

    private Mode requestMode = Mode.QUEUEING;
    private Mode responseMode = Mode.QUEUEING;
    private Task task = Task.NONE;

    private RequestLineToken requestLineToken;
    private RequestLineToken responseRequest;
    private StatusLine statusLine;

    private Token[] requestResponse = null;
    private Token[] responseResponse = null;

    private TokenStreamer preStreamer = null;
    private TokenStreamer postStreamer = null;

    private boolean requestPersistent;
    private boolean responsePersistent;

    private final NodeTCPSession session;

    // constructors -----------------------------------------------------------

    protected HttpStateMachine(NodeTCPSession session)
    {
        super(session);
        this.session = session;
    }

    // protected abstract methods ---------------------------------------------

    // XXX the default impls should pass through?

    protected abstract RequestLineToken doRequestLine(RequestLineToken rl)
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

    protected RequestLineToken getRequestLine()
    {
        return requestLineToken;
    }

    protected RequestLineToken getResponseRequest()
    {
        return responseRequest;
    }

    protected StatusLine getStatusLine()
    {
        return statusLine;
    }

    protected String getResponseHost()
    {
        return hosts.get(responseRequest);
    }

    protected boolean isRequestPersistent()
    {
        return requestPersistent;
    }

    protected boolean isResponsePersistent()
    {
        return responsePersistent;
    }

    protected Mode getRequestMode()
    {
        return requestMode;
    }

    protected Mode getResponseMode()
    {
        return responseMode;
    }

    protected void releaseRequest()
    {
        if (Task.REQUEST != task) {
            throw new IllegalStateException("releaseRequest in: " + task);
        } else if (Mode.QUEUEING != requestMode) {
            throw new IllegalStateException("releaseRequest in: " + requestMode);
        }

        requestMode = Mode.RELEASED;
    }

    protected void preStream(TokenStreamer preStreamer)
    {
        switch (task) {
        case REQUEST:
            if (Mode.RELEASED != requestMode) {
                throw new IllegalStateException("preStream in: " + requestMode);
            } else if (ClientState.REQ_BODY_STATE != clientState
                       && ClientState.REQ_BODY_END_STATE != clientState) {
                throw new IllegalStateException("preStream in: " + clientState);
            } else {
                this.preStreamer = preStreamer;
            }
            break;

        case RESPONSE:
            if (Mode.RELEASED != responseMode) {
                throw new IllegalStateException("preStream in: " + responseMode);
            } else if (ServerState.RESP_BODY_STATE != serverState
                       && ServerState.RESP_BODY_END_STATE != serverState) {
                throw new IllegalStateException("preStream in: " + serverState);
            } else {
                this.preStreamer = preStreamer;
            }
            break;

        case NONE:
            throw new IllegalStateException("stream in: " + task);

        default:
            throw new IllegalStateException("programmer malfunction");
        }
    }

    protected void postStream(TokenStreamer postStreamer)
    {
        switch (task) {
        case REQUEST:
            if (Mode.RELEASED != requestMode) {
                throw new IllegalStateException("postStream in: " + requestMode);
            } else if (ClientState.REQ_HEADER_STATE != clientState
                       && ClientState.REQ_BODY_STATE != clientState) {
                throw new IllegalStateException("postStream in: " + clientState);
            } else {
                this.postStreamer = postStreamer;
            }
            break;

        case RESPONSE:
            if (Mode.RELEASED != responseMode) {
                throw new IllegalStateException("postStream in: " + responseMode);
            } else if (ServerState.RESP_HEADER_STATE != serverState
                       && ServerState.RESP_BODY_STATE != serverState) {
                throw new IllegalStateException("postStream in: " + serverState);
            } else {
                this.postStreamer = postStreamer;
            }
            break;

        case NONE:
            throw new IllegalStateException("stream in: " + task);

        default:
            throw new IllegalStateException("programmer malfunction");
        }
    }

    protected void blockRequest(Token[] response)
    {
        if (Task.REQUEST != task) {
            throw new IllegalStateException("blockRequest in: " + task);
        } else if (Mode.QUEUEING != requestMode) {
            throw new IllegalStateException("blockRequest in: " + requestMode);
        }

        requestMode = Mode.BLOCKED;
        requestResponse = response;
    }

    protected void releaseResponse()
    {
        if (Task.RESPONSE != task) {
            throw new IllegalStateException("releaseResponse in: " + task);
        } else if (Mode.QUEUEING != responseMode) {
            throw new IllegalStateException("releaseResponse in: " + responseMode);
        }

        responseMode = Mode.RELEASED;
    }

    protected void blockResponse(Token[] response)
    {
        if (Task.RESPONSE != task) {
            throw new IllegalStateException("blockResponse in: " + task);
        } else if (Mode.QUEUEING != responseMode) {
            throw new IllegalStateException("blockResponse in: " + responseMode);
        }

        responseMode = Mode.BLOCKED;
        responseResponse = response;
    }


    // AbstractTokenHandler methods -------------------------------------------

    public TokenResult handleClientToken(Token token) throws TokenException
    {
        TokenResult tr;

        task = Task.REQUEST;
        try {
            tr = doHandleClientToken(token);

            if (null != preStreamer || null != postStreamer) {
                Token[] s2cToks = tr.s2cTokens();
                TokenStreamer s2c = new ArrayTokenStreamer(s2cToks, false);

                List<TokenStreamer> l = new ArrayList<TokenStreamer>(3);
                if (null != preStreamer) {
                    l.add(preStreamer);
                }
                Token[] c2sToks = tr.c2sTokens();
                TokenStreamer c2sAts = new ArrayTokenStreamer(c2sToks, false);
                l.add(c2sAts);
                if (null != postStreamer) {
                    l.add(postStreamer);
                }
                TokenStreamer c2s = new SeriesTokenStreamer(l);

                tr = new TokenResult(s2c, c2s);
            }
        } finally {
            task = Task.NONE;
            preStreamer = postStreamer = null;
        }

        return tr;
    }

    public TokenResult handleServerToken(Token token) throws TokenException
    {
        TokenResult tr;

        task = Task.RESPONSE;
        try {
            tr = doHandleServerToken(token);

            if (null != preStreamer || null != postStreamer) {
                List<TokenStreamer> l = new ArrayList<TokenStreamer>(3);
                if (null != preStreamer) {
                    l.add(preStreamer);
                }

                Token[] s2cToks = tr.s2cTokens();
                TokenStreamer s2cAts = new ArrayTokenStreamer(s2cToks, false);
                l.add(s2cAts);
                if (null != postStreamer) {
                    l.add(postStreamer);
                }

                TokenStreamer s2c = new SeriesTokenStreamer(l);

                Token[] c2sToks = tr.c2sTokens();
                TokenStreamer c2s = new ArrayTokenStreamer(c2sToks, false);

                tr = new TokenResult(s2c, c2s);
            }
        } finally {
            task = Task.NONE;
            preStreamer = postStreamer = null;
        }

        return tr;
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

    @SuppressWarnings("fallthrough")
    private TokenResult doHandleClientToken(Token token) throws TokenException
    {
        clientState = nextClientState(token);

        switch (clientState) {
        case REQ_LINE_STATE:
            requestMode = Mode.QUEUEING;
            requestLineToken = (RequestLineToken)token;
            requestLineToken = doRequestLine(requestLineToken);

            switch (requestMode) {
            case QUEUEING:
                requestQueue.add(requestLineToken);
                return TokenResult.NONE;

            case RELEASED:
                assert 0 == requestQueue.size();

                requests.add(requestLineToken);
                outstandingResponses.add(null);
                return new TokenResult(null, new Token[] { requestLineToken } );

            case BLOCKED:
                assert 0 == requestQueue.size();

                if (0 == requests.size()) {
                    TokenResult tr = new TokenResult(requestResponse, null);
                    requestResponse = null;
                    return tr;
                } else {
                    requests.add(requestLineToken);
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
                Mode preMode = requestMode;
                h = doRequestHeader(h);

                String host = h.getValue("host");

                hosts.put(requestLineToken, host);

                /**
                 * Attach metadata
                 */
                this.session.globalAttach(NodeSession.KEY_HTTP_HOSTNAME,host);
                String uri = getRequestLine().getRequestUri().normalize().getPath();
                this.session.globalAttach(NodeSession.KEY_HTTP_URI,uri);

                switch (requestMode) {
                case QUEUEING:
                    requestQueue.add(h);
                    return TokenResult.NONE;

                case RELEASED:
                    requestQueue.add(h);
                    Token[] toks = new Token[requestQueue.size()];
                    toks = requestQueue.toArray(toks);
                    requestQueue.clear();
                    if (Mode.QUEUEING == preMode) {
                        requests.add(requestLineToken);
                        outstandingResponses.add(null);
                    }
                    return new TokenResult(null, toks);

                case BLOCKED:
                    requestQueue.clear();

                    if (0 == requests.size()) {
                        TokenResult tr = new TokenResult(requestResponse, null);
                        requestResponse = null;
                        return tr;
                    } else {
                        requests.add(requestLineToken);
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
                Mode preMode = requestMode;
                c = doRequestBody(c);

                switch (requestMode) {
                case QUEUEING:
                    if (null != c && Chunk.EMPTY != c) {
                        requestQueue.add(c);
                    }
                    return TokenResult.NONE;

                case RELEASED:
                    if (null != c && Chunk.EMPTY != c) {
                        requestQueue.add(c);
                    }
                    Token[] toks = new Token[requestQueue.size()];
                    toks = requestQueue.toArray(toks);
                    requestQueue.clear();
                    if (Mode.QUEUEING == preMode) {
                        requests.add(requestLineToken);
                        outstandingResponses.add(null);
                    }
                    return new TokenResult(null, toks);

                case BLOCKED:
                    requestQueue.clear();

                    if (0 == requests.size()) {
                        TokenResult tr = new TokenResult(requestResponse, null);
                        requestResponse = null;
                        return tr;
                    } else {
                        requests.add(requestLineToken);
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
                Mode preMode = requestMode;
                doRequestBodyEnd();

                switch (requestMode) {
                case QUEUEING:
                    logger.error("queueing after EndMarker, release request");
                    releaseRequest();
                    /* fall through */

                case RELEASED:
                    doRequestBodyEnd();

                    requestQueue.add(EndMarker.MARKER);
                    Token[] toks = new Token[requestQueue.size()];
                    toks = requestQueue.toArray(toks);
                    requestQueue.clear();
                    if (Mode.QUEUEING == preMode) {
                        requests.add(requestLineToken);
                        outstandingResponses.add(null);
                    }
                    return new TokenResult(null, toks);

                case BLOCKED:
                    requestQueue.clear();

                    if (0 == requests.size()) {
                        TokenResult tr = new TokenResult(requestResponse, null);
                        requestResponse = null;
                        return tr;
                    } else {
                        requests.add(requestLineToken);
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

    @SuppressWarnings("fallthrough")
    private TokenResult doHandleServerToken(Token token) throws TokenException
    {
        serverState = nextServerState(token);

        switch (serverState) {
        case RESP_STATUS_LINE_STATE:
            responseMode = Mode.QUEUEING;

            statusLine = (StatusLine)token;
            int sc = statusLine.getStatusCode();
            if (100 != sc && 408 != sc) { /* not continue or request timed out */
                if (0 == requests.size()) {
                    if (4 != sc / 100) {
                        logger.warn("requests is empty, code: " + sc);
                    }
                } else {
                    responseRequest = requests.remove(0);
                    responseResponse = outstandingResponses.remove(0);
                }
            }

            if (null == responseResponse) {
                statusLine = doStatusLine(statusLine);
            }

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
                    if (null != c && Chunk.EMPTY != c) {
                        responseQueue.add(c);
                    }
                    return TokenResult.NONE;

                case RELEASED:
                    if (null != c && Chunk.EMPTY != c) {
                        responseQueue.add(c);
                    }
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
                if (100 != statusLine.getStatusCode()) {
                    hosts.remove(responseRequest);
                }

                switch (responseMode) {
                case QUEUEING:
                    logger.warn("queueing after EndMarker, release repsonse");
                    releaseResponse();
                    /* fall through */

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
