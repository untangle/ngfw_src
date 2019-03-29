/**
 * $Id$
 */
package com.untangle.app.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.EndMarkerToken;
import com.untangle.app.http.HeaderToken;
import com.untangle.uvm.vnet.ReleaseToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.TokenStreamer;
import com.untangle.uvm.vnet.TokenStreamerAdaptor;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.TCPStreamer;
import com.untangle.uvm.vnet.AbstractEventHandler;

/**
 * Adapts a stream of HTTP tokens to methods relating to the protocol
 * state.
 *
 * HttpEventHandler provides the base class that http-processing apps
 * override. It handles the basic handling of http processing and will
 * call abstract methods when interesting events happen.
 */
public abstract class HttpEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String SESSION_STATE_KEY = "http-session-state";

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

    /**
     * HttpSessionState stores all the HTTP session state of a given session
     */
    private class HttpSessionState
    {
        protected final List<RequestLineToken> requests = new LinkedList<>();

        protected final List<Token[]> outstandingResponses = new LinkedList<>();

        protected final List<Token> requestQueue = new ArrayList<>();
        protected final List<Token> responseQueue = new ArrayList<>();
        protected final Map<RequestLineToken, String> hosts = new HashMap<>();

        protected ClientState clientState = ClientState.REQ_START_STATE;
        protected ServerState serverState = ServerState.RESP_START_STATE;

        protected Mode requestMode = Mode.QUEUEING;
        protected Mode responseMode = Mode.QUEUEING;
        protected Task task = Task.NONE;

        protected RequestLineToken requestLineToken;
        protected RequestLineToken responseRequest;
        protected StatusLine statusLine;

        protected Token[] requestResponse = null;
        protected Token[] responseResponse = null;

        protected TokenStreamer preStreamer = null;

        protected boolean requestPersistent;
        protected boolean responsePersistent;
    }

    /**
     * Create a new HttpEventHandler.
     */
    protected HttpEventHandler()
    {
        super();
    }

    /**
     * doRequestLine should be overridden by the final class to determine how RequestLineTokens are handled
     * @param session - the session
     * @param rl  - the RequestLineToken
     * @return the RequestLineToken to pass down the pipeline (or null)
     */
    protected abstract RequestLineToken doRequestLine( AppTCPSession session, RequestLineToken rl );

    /**
     * doRequestHeader should be overridden by the final class to determine how HeaderTokens are handled
     * @param session  - the session
     * @param h  - the HeaderToken
     * @return the HeaderToken to pass down the pipeline (or null)
     */
    protected abstract HeaderToken doRequestHeader( AppTCPSession session, HeaderToken h );

    /**
     * doRequestBody should be overridden by the final class to determine how ChunkTokens are handled
     * @param session - the session
     * @param c - the ChunkToken
     * @return the ChunkToken to pass down the pipeline (or null)
     */
    protected abstract ChunkToken doRequestBody( AppTCPSession session, ChunkToken c );

    /**
     * doRequestBodyEnd should be overridden by the final class to determine how the end of the body is handled
     * @param session - the session
     */
    protected abstract void doRequestBodyEnd( AppTCPSession session );

    /**
     * doStatusLine should be overridden by the final class to determine how StatusLine is handled
     * @param session - the session
     * @param sl - the StatusLine
     * @return the StatusLine to pass down the pipeline (or null)
     */
    protected abstract StatusLine doStatusLine( AppTCPSession session, StatusLine sl );

    /**
     * doResponseHeader should be overridden by the final class to determine how HeaderTokens are handled
     * @param session - the session
     * @param h - the HeaderToken
     * @return the HeaderToken to pass down the pipeline (or null)
     */
    protected abstract HeaderToken doResponseHeader( AppTCPSession session, HeaderToken h );

    /**
     * doResponseBody should be overridden by the final class to determine how ChunkTokens are handled
     * @param session - the session
     * @param c - the ChunkToken
     * @return the ChunkToken to pass down the pipeline (or null)
     */
    protected abstract ChunkToken doResponseBody( AppTCPSession session, ChunkToken c );

    /**
     * doResponseBodyEnd should be overridden by the final class to determine how the end of the body is handled
     * @param session - the session
     */
    protected abstract void doResponseBodyEnd( AppTCPSession session );

    /**
     * Get the ClientState for the session
     * @param session - the session
     * @return ClientState
     */
    protected ClientState getClientState( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.clientState;
    }

    /**
     * Get the ServerState for the session
     * @param session - the session
     * @return ServerState
     */
    protected ServerState getServerState( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.serverState;
    }

    /**
     * get RequestLine for the session
     * @param session
     * @return RequestLine
     */
    protected RequestLineToken getRequestLine( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.requestLineToken;
    }

    /**
     * get RequestLineToken for the session
     * @param session
     * @return RequestLineToken
     */
    protected RequestLineToken getResponseRequest( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.responseRequest;
    }

    /**
     * get the StatusLine for the session
     * @param session
     * @return StatusLine
     */
    protected StatusLine getStatusLine( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.statusLine;
    }

    /**
     * Get the Response Host for the session
     * @param session
     * @return response host as a string
     */
    protected String getResponseHost( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.hosts.get( state.responseRequest );
    }

    /**
     * get requestPersistent for the session
     * @param session
     * @return boolean
     */
    protected boolean isRequestPersistent( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.requestPersistent;
    }

    /**
     * get pesponsePersistent for the session
     * @param session
     * @return boolean
     */
    protected boolean isResponsePersistent( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.responsePersistent;
    }

    /**
     * get requestMode for the session
     * @param session
     * @return Mode
     */
    protected Mode getRequestMode( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.requestMode;
    }

    /**
     * get responseMode for the session
     * @param session
     * @return Mode
     */
    protected Mode getResponseMode( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.responseMode;
    }

    /**
     * Release the specified session
     * @param session
     */
    protected void releaseRequest( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        if ( state.task != Task.REQUEST ) {
            throw new IllegalStateException("releaseRequest in: " + state.task);
        } else if ( state.requestMode != Mode.QUEUEING ) {
            throw new IllegalStateException("releaseRequest in: " + state.requestMode);
        }

        state.requestMode = Mode.RELEASED;
    }

    /**
     * Stream the specified TokenStreamer's data to the client
     * @param session
     * @param streamer
     */
    protected void streamClient( AppTCPSession session, TokenStreamer streamer )
    {
        stream( session, AppSession.CLIENT, streamer );
    }

    /**
     * Stream the specified TokenStreamer's data to the server
     * @param session
     * @param streamer
     */
    protected void streamServer( AppTCPSession session, TokenStreamer streamer )
    {
        stream( session, AppSession.SERVER, streamer );
    }

    /**
     * Stream the specified TokenStreamer's data to the server/client
     * @param session
     * @param side - client/server
     * @param streamer
     */
    protected void stream( AppTCPSession session, int side, TokenStreamer streamer )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );

        switch ( state.task ) {
        case REQUEST:
            if ( state.requestMode != Mode.RELEASED ) {
                throw new IllegalStateException("preStream in: " + state.requestMode);
            } else if ( ClientState.REQ_BODY_STATE != state.clientState && ClientState.REQ_BODY_END_STATE != state.clientState ) {
                throw new IllegalStateException("preStream in: " + state.clientState);
            } else {
                break;
            }
        case RESPONSE:
            if ( state.responseMode != Mode.RELEASED ) {
                throw new IllegalStateException("preStream in: " + state.responseMode);
            } else if ( ServerState.RESP_BODY_STATE != state.serverState && ServerState.RESP_BODY_END_STATE != state.serverState ) {
                throw new IllegalStateException("preStream in: " + state.serverState);
            } else {
                break;
            }
        case NONE:
            throw new IllegalStateException("stream in: " + state.task);

        default:
            throw new IllegalStateException("programmer malfunction");
        }

        TCPStreamer tcpStreamer = new TokenStreamerAdaptor( streamer, session );
        session.sendStreamer( side, tcpStreamer );
    }

    /**
     * Block the request and return the specified response
     * @param session
     * @param response
     */
    protected void blockRequest( AppTCPSession session, Token[] response)
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );

        if ( state.task != Task.REQUEST ) {
            throw new IllegalStateException("blockRequest in: " + state.task);
        } else if ( state.requestMode != Mode.QUEUEING ) {
            throw new IllegalStateException("blockRequest in: " + state.requestMode);
        }

        state.requestMode = Mode.BLOCKED;
        state.requestResponse = response;
    }

    /**
     * Release the session
     * @param session
     */
    protected void releaseResponse( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );

        if ( state.task != Task.RESPONSE ) {
            throw new IllegalStateException("releaseResponse in: " + state.task);
        } else if ( state.responseMode != Mode.QUEUEING ) {
            throw new IllegalStateException("releaseResponse in: " + state.responseMode);
        }

        state.responseMode = Mode.RELEASED;
    }

    /**
     * Block the response and replace with the specified response
     * @param session
     * @param response
     */
    protected void blockResponse( AppTCPSession session, Token[] response )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );

        if ( state.task != Task.RESPONSE ) {
            throw new IllegalStateException("blockResponse in: " + state.task);
        } else if ( state.responseMode != Mode.QUEUEING ) {
            throw new IllegalStateException("blockResponse in: " + state.responseMode);
        }

        state.responseMode = Mode.BLOCKED;
        state.responseResponse = response;
    }

    /**
     * handleTCPNewSession.
     * @param session
     */
    @Override
    public void handleTCPNewSession( AppTCPSession session )
    {
        HttpSessionState state = new HttpSessionState();
        session.attach( SESSION_STATE_KEY, state );
    }

    /**
     * handleTCPServerObject.
     * @param session
     * @param obj
     */
    @Override
    public void handleTCPServerObject( AppTCPSession session, Object obj )
    {
        Token token = (Token) obj;
        if (token instanceof ReleaseToken) {
            session.sendObjectToClient( token );
            releaseFlush( session );
            handleTCPFinalized( session );
            session.release();
            return;
        }

        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );

        state.task = Task.RESPONSE;
        try {
            doHandleServerToken( session, token );
        } finally {
            state.task = Task.NONE;
            state.preStreamer = null;
        }
    }

    /**
     * handleTCPClientObject.
     * @param session
     * @param obj
     */
    @Override
    public void handleTCPClientObject( AppTCPSession session, Object obj )
    {
        Token token = (Token) obj;
        if (token instanceof ReleaseToken) {
            session.sendObjectToServer( token );
            releaseFlush( session );
            handleTCPFinalized( session );
            session.release();
            return;
        }

        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );

        state.task = Task.REQUEST;
        try {
            doHandleClientToken( session, token );

            if ( state.preStreamer != null ) {
                return;
            }
        } finally {
            state.task = Task.NONE;
            state.preStreamer = null;
        }
    }
    
    /**
     * Flush all queued data to client and server
     * @param session
     */
    public void releaseFlush( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );

        // XXX we do not even attempt to deal with outstanding block pages
        Token[] req = new Token[state.requestQueue.size()];
        req = state.requestQueue.toArray(req);
        Token[] resp = new Token[state.responseQueue.size()];
        resp = state.responseQueue.toArray(resp);
        for ( Token tok : resp ) {
            session.sendDataToClient( tok.getBytes() );
        }
        for ( Token tok : req ) {
            session.sendDataToServer( tok.getBytes() );
        }
        return;
    }

    /**
     * Handle the specified token in the HTTP state machine
     * This handles the core logic of the HttpEventHandler
     * and will call the specified abstract methods when events happen
     * @param session
     * @param token
     */
    @SuppressWarnings("fallthrough")
    private void doHandleClientToken( AppTCPSession session, Token token )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        state.clientState = nextClientState( state.clientState, token );

        switch (state.clientState) {
        case REQ_LINE_STATE:
            state.requestMode = Mode.QUEUEING;
            state.requestLineToken = (RequestLineToken)token;
            state.requestLineToken = doRequestLine( session, state.requestLineToken );

            switch ( state.requestMode ) {
            case QUEUEING:
                state.requestQueue.add( state.requestLineToken );
                return;

            case RELEASED:
                state.requests.add( state.requestLineToken );
                state.outstandingResponses.add(null);
                session.sendObjectToServer( state.requestLineToken );
                return;

            case BLOCKED:
                if ( state.requests.size() == 0 ) {
                    session.sendObjectsToClient( state.requestResponse );
                    state.requestResponse = null;
                    return;
                } else {
                    state.requests.add( state.requestLineToken );
                    state.outstandingResponses.add( state.requestResponse );
                    state.requestResponse = null;
                    return;
                }

            default:
                throw new IllegalStateException("programmer malfunction");
            }

        case REQ_HEADER_STATE:
            if ( state.requestMode != Mode.BLOCKED ) {
                HeaderToken h = (HeaderToken)token;
                state.requestPersistent = isPersistent(h);
                Mode preMode = state.requestMode;

                h = doRequestHeader( session, h );

                String host = h.getValue("host");
                state.hosts.put( state.requestLineToken, host );
                
                switch ( state.requestMode ) {
                case QUEUEING:
                    state.requestQueue.add(h);
                    return;

                case RELEASED:
                    state.requestQueue.add(h);
                    Token[] toks = new Token[ state.requestQueue.size() ];
                    toks = state.requestQueue.toArray(toks);
                    state.requestQueue.clear();
                    if ( preMode == Mode.QUEUEING ) {
                        state.requests.add( state.requestLineToken );
                        state.outstandingResponses.add(null);
                    }
                    session.sendObjectsToServer( toks );
                    return;

                case BLOCKED:
                    state.requestQueue.clear();

                    if ( state.requests.size() == 0 ) {
                        session.sendObjectsToClient( state.requestResponse );
                        state.requestResponse = null;
                        return;
                    } else {
                        state.requests.add( state.requestLineToken );
                        state.outstandingResponses.add( state.requestResponse );
                        state.requestResponse = null;
                        return;
                    }

                default:
                    throw new IllegalStateException("programmer malfunction");
                }
            } else {
                return;
            }

        case REQ_BODY_STATE:
            if ( state.requestMode != Mode.BLOCKED ) {
                ChunkToken c = (ChunkToken) token;
                Mode preMode = state.requestMode;
                c = doRequestBody( session, c );

                switch ( state.requestMode ) {
                case QUEUEING:
                    if ( c != null && c != ChunkToken.EMPTY ) {
                        state.requestQueue.add(c);
                    }
                    return;

                case RELEASED:
                    if ( c != null  && c != ChunkToken.EMPTY ) {
                        state.requestQueue.add(c);
                    }
                    Token[] toks = new Token[ state.requestQueue.size() ];
                    toks = state.requestQueue.toArray(toks);
                    state.requestQueue.clear();
                    if ( preMode == Mode.QUEUEING ) {
                        state.requests.add( state.requestLineToken );
                        state.outstandingResponses.add(null);
                    }
                    session.sendObjectsToServer( toks );
                    return;

                case BLOCKED:
                    state.requestQueue.clear();

                    if ( state.requests.size() == 0 ) {
                        session.sendObjectsToClient( state.requestResponse );
                        state.requestResponse = null;
                        return;
                    } else {
                        state.requests.add( state.requestLineToken );
                        state.outstandingResponses.add( state.requestResponse );
                        state.requestResponse = null;
                        return;
                    }

                default:
                    throw new IllegalStateException("programmer malfunction");
                }

            } else {
                return;
            }

        case REQ_BODY_END_STATE:
            if ( state.requestMode != Mode.BLOCKED ) {
                Mode preMode = state.requestMode;
                doRequestBodyEnd( session );

                switch ( state.requestMode ) {
                case QUEUEING:
                    logger.error("queueing after EndMarkerToken, release request");
                    releaseRequest( session );
                    /* fall through */

                case RELEASED:
                    doRequestBodyEnd( session );

                    state.requestQueue.add(EndMarkerToken.MARKER);
                    Token[] toks = new Token[ state.requestQueue.size() ];
                    toks = state.requestQueue.toArray(toks);
                    state.requestQueue.clear();
                    if ( preMode == Mode.QUEUEING ) {
                        state.requests.add( state.requestLineToken );
                        state.outstandingResponses.add(null);
                    }
                    session.sendObjectsToServer( toks );
                    return;

                case BLOCKED:
                    state.requestQueue.clear();

                    if ( state.requests.size() == 0 ) {
                        session.sendObjectsToClient( state.requestResponse );
                        state.requestResponse = null;
                        return;
                    } else {
                        state.requests.add( state.requestLineToken );
                        state.outstandingResponses.add( state.requestResponse );
                        state.requestResponse = null;
                        return;
                    }

                default:
                    throw new IllegalStateException("programmer malfunction");
                }
            } else {
                return;
            }

        default:
            throw new IllegalStateException("programmer malfunction");
        }
    }

    /**
     * Handle a token from the server
     * @param session
     * @param token
     */
    @SuppressWarnings("fallthrough")
    private void doHandleServerToken( AppTCPSession session, Token token )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        state.serverState = nextServerState( state.serverState, token );

        switch ( state.serverState ) {
        case RESP_STATUS_LINE_STATE:
            state.responseMode = Mode.QUEUEING;

            state.statusLine = (StatusLine)token;
            int sc = state.statusLine.getStatusCode();
            if ( sc != 100 && sc != 408 ) { /* not continue or request timed out */
                if ( state.requests.size() == 0 ) {
                    if ( sc / 100 != 4 && sc != 200 && sc != 503 ) {
                        logger.warn("requests is empty, code: " + sc);
                    }
                } else {
                    state.responseRequest = state.requests.remove(0);
                    state.responseResponse = state.outstandingResponses.remove(0);
                }
            }

            if ( state.responseResponse == null ) {
                state.statusLine = doStatusLine( session, state.statusLine );
            }

            switch ( state.responseMode ) {
            case QUEUEING:
                state.responseQueue.add( state.statusLine );
                return;

            case RELEASED:
                if ( state.responseQueue.size() != 0 )
                    logger.warn("Invalid response queue size on release: " + state.responseQueue.size());

                session.sendObjectToClient( state.statusLine );
                return;

            case BLOCKED:
                if ( state.responseQueue.size() != 0 )
                    logger.warn("Invalid response queue size on block: " + state.responseQueue.size());

                session.sendObjectsToClient( state.responseResponse );
                state.responseResponse = null;
                return;

            default:
                throw new IllegalStateException("programmer malfunction");
            }

        case RESP_HEADER_STATE:
            if ( state.responseMode != Mode.BLOCKED ) {
                HeaderToken h = (HeaderToken)token;
                state.responsePersistent = isPersistent(h);
                h = doResponseHeader( session, h );

                switch ( state.responseMode ) {
                case QUEUEING:
                    state.responseQueue.add(h);
                    return;

                case RELEASED:
                    state.responseQueue.add(h);
                    Token[] toks = new Token[ state.responseQueue.size() ];
                    toks = state.responseQueue.toArray(toks);
                    state.responseQueue.clear();
                    session.sendObjectsToClient( toks );
                    return;

                case BLOCKED:
                    state.responseQueue.clear();
                    session.sendObjectsToClient( state.responseResponse );
                    state.responseResponse = null;
                    return;

                default:
                    throw new IllegalStateException("programmer malfunction");
                }
            } else {
                return;
            }

        case RESP_BODY_STATE:
            if ( state.responseMode != Mode.BLOCKED ) {
                ChunkToken c = (ChunkToken)token;
                c = doResponseBody( session, c );

                switch ( state.responseMode ) {
                case QUEUEING:
                    if (null != c && ChunkToken.EMPTY != c) {
                        state.responseQueue.add(c);
                    }
                    return;

                case RELEASED:
                    if (null != c && ChunkToken.EMPTY != c) {
                        state.responseQueue.add(c);
                    }
                    Token[] toks = new Token[ state.responseQueue.size() ];
                    toks = state.responseQueue.toArray(toks);
                    state.responseQueue.clear();
                    session.sendObjectsToClient( toks );
                    return;

                case BLOCKED:
                    state.responseQueue.clear();
                    session.sendObjectsToClient( state.responseResponse );
                    state.responseResponse = null;
                    return;

                default:
                    throw new IllegalStateException("programmer malfunction");
                }
            } else {
                return;
            }

        case RESP_BODY_END_STATE:
            if ( state.responseMode != Mode.BLOCKED ) {
                EndMarkerToken em = (EndMarkerToken)token;

                doResponseBodyEnd( session );
                if ( state.statusLine.getStatusCode() != 100 ) {
                    state.hosts.remove( state.responseRequest );
                }

                switch ( state.responseMode ) {
                case QUEUEING:
                    logger.warn("queueing after EndMarkerToken, release repsonse");
                    releaseResponse( session );
                    /* fall through */

                case RELEASED:
                    state.responseQueue.add(em);
                    Token[] toks = new Token[ state.responseQueue.size() ];
                    toks = state.responseQueue.toArray(toks);
                    state.responseQueue.clear();
                    session.sendObjectsToClient( toks );
                    return;

                case BLOCKED:
                    state.responseQueue.clear();
                    session.sendObjectsToClient( state.responseResponse );
                    state.responseResponse = null;
                    return;

                default:
                    throw new IllegalStateException("programmer malfunction");
                }

            } else {
                return;
            }

        default:
            throw new IllegalStateException("programmer malfunction");
        }
    }

    /**
     * Iterate to the next client state from the specified clientState
     * @param clientState
     * @param o - the ChunkToken
     * @return the next ClientState
     */
    private ClientState nextClientState( ClientState clientState, Object o )
    {
        switch ( clientState ) {
        case REQ_START_STATE:
            return ClientState.REQ_LINE_STATE;

        case REQ_LINE_STATE:
            return ClientState.REQ_HEADER_STATE;

        case REQ_HEADER_STATE:
            if (o instanceof ChunkToken) {
                return ClientState.REQ_BODY_STATE;
            } else {
                return ClientState.REQ_BODY_END_STATE;
            }

        case REQ_BODY_STATE:
            if (o instanceof ChunkToken) {
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

    /**
     * Iterate to the next server state from the specified serverState
     * @param serverState
     * @param o - the ChunkToken
     * @return the next ServerState
     */
    private ServerState nextServerState( ServerState serverState, Object o)
    {
        switch ( serverState ) {
        case RESP_START_STATE: {
            return ServerState.RESP_STATUS_LINE_STATE;
        }

        case RESP_STATUS_LINE_STATE: {
            return ServerState.RESP_HEADER_STATE;
        }

        case RESP_HEADER_STATE:
            if (o instanceof ChunkToken) {
                return ServerState.RESP_BODY_STATE;
            } else {
                return ServerState.RESP_BODY_END_STATE;
            }

        case RESP_BODY_STATE:
            if (o instanceof ChunkToken) {
                return ServerState.RESP_BODY_STATE;
            } else {
                return ServerState.RESP_BODY_END_STATE;
            }

        case RESP_BODY_END_STATE: {
            return ServerState.RESP_STATUS_LINE_STATE;
        }

        default:
            throw new IllegalStateException("Illegal state: " + serverState);
        }
    }

    /**
     * Returns true if this is a persistent connection
     * calculated by the presence of a connection=keep-alive header
     * @param header - the HeaderToken
     * @return true if persistent, false otherwise
     */
    private boolean isPersistent(HeaderToken header)
    {
        String con = header.getValue("connection");
        return null == con ? false : con.equalsIgnoreCase("keep-alive");
    }
}
