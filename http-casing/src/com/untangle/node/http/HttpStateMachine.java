/**
 * $Id$
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
import com.untangle.node.token.TokenStreamer;
import com.untangle.node.token.TokenStreamerAdaptor;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.TCPStreamer;

/**
 * Adapts a stream of HTTP tokens to methods relating to the protocol
 * state.
 *
 */
public abstract class HttpStateMachine extends AbstractTokenHandler
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String SESSION_STATE_KEY = "HTTP-session-state";

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

    private class HttpSessionState
    {
        protected final List<RequestLineToken> requests = new LinkedList<RequestLineToken>();

        protected final List<Token[]> outstandingResponses = new LinkedList<Token[]>();

        protected final List<Token> requestQueue = new ArrayList<Token>();
        protected final List<Token> responseQueue = new ArrayList<Token>();
        protected final Map<RequestLineToken, String> hosts = new HashMap<RequestLineToken, String>();

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

    // constructors -----------------------------------------------------------

    protected HttpStateMachine()
    {
        super();
    }

    // protected abstract methods ---------------------------------------------

    protected abstract RequestLineToken doRequestLine( NodeTCPSession session, RequestLineToken rl )
        throws TokenException;
    protected abstract Header doRequestHeader( NodeTCPSession session, Header h )
        throws TokenException;
    protected abstract Chunk doRequestBody( NodeTCPSession session, Chunk c )
        throws TokenException;
    protected abstract void doRequestBodyEnd( NodeTCPSession session )
        throws TokenException;

    protected abstract StatusLine doStatusLine( NodeTCPSession session, StatusLine sl )
        throws TokenException;
    protected abstract Header doResponseHeader( NodeTCPSession session, Header h )
        throws TokenException;
    protected abstract Chunk doResponseBody( NodeTCPSession session, Chunk c )
        throws TokenException;
    protected abstract void doResponseBodyEnd( NodeTCPSession session )
        throws TokenException;

    // protected methods ------------------------------------------------------

    protected ClientState getClientState( NodeTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.clientState;
    }

    protected ServerState getServerState( NodeTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.serverState;
    }

    protected RequestLineToken getRequestLine( NodeTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.requestLineToken;
    }

    protected RequestLineToken getResponseRequest( NodeTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.responseRequest;
    }

    protected StatusLine getStatusLine( NodeTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.statusLine;
    }

    protected String getResponseHost( NodeTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.hosts.get( state.responseRequest );
    }

    protected boolean isRequestPersistent( NodeTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.requestPersistent;
    }

    protected boolean isResponsePersistent( NodeTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.responsePersistent;
    }

    protected Mode getRequestMode( NodeTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.requestMode;
    }

    protected Mode getResponseMode( NodeTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.responseMode;
    }

    protected void releaseRequest( NodeTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        if ( state.task != Task.REQUEST ) {
            throw new IllegalStateException("releaseRequest in: " + state.task);
        } else if ( state.requestMode != Mode.QUEUEING ) {
            throw new IllegalStateException("releaseRequest in: " + state.requestMode);
        }

        state.requestMode = Mode.RELEASED;
    }

    protected void streamClient( NodeTCPSession session, TokenStreamer streamer )
    {
        stream( session, NodeSession.CLIENT, streamer );
    }

    protected void streamServer( NodeTCPSession session, TokenStreamer streamer )
    {
        stream( session, NodeSession.SERVER, streamer );
    }

    protected void stream( NodeTCPSession session, int side, TokenStreamer streamer )
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
        session.beginStream( side, tcpStreamer );
    }

    protected void blockRequest( NodeTCPSession session, Token[] response)
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

    protected void releaseResponse( NodeTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );

        if ( state.task != Task.RESPONSE ) {
            throw new IllegalStateException("releaseResponse in: " + state.task);
        } else if ( state.responseMode != Mode.QUEUEING ) {
            throw new IllegalStateException("releaseResponse in: " + state.responseMode);
        }

        state.responseMode = Mode.RELEASED;
    }

    protected void blockResponse( NodeTCPSession session, Token[] response )
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

    // AbstractTokenHandler methods -------------------------------------------

    @Override
    public void handleNewSession( NodeTCPSession session )
    {
        HttpSessionState state = new HttpSessionState();
        session.attach( SESSION_STATE_KEY, state );
    }
    
    @Override
    public void handleClientToken( NodeTCPSession session, Token token ) throws TokenException
    {
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

        return;
    }

    public void handleServerToken( NodeTCPSession session, Token token ) throws TokenException
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );

        state.task = Task.RESPONSE;
        try {
            doHandleServerToken( session, token);
        } finally {
            state.task = Task.NONE;
            state.preStreamer = null;
        }

        return;
    }

    @Override
    public void releaseFlush( NodeTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );

        // XXX we do not even attempt to deal with outstanding block pages
        Token[] req = new Token[state.requestQueue.size()];
        req = state.requestQueue.toArray(req);
        Token[] resp = new Token[state.responseQueue.size()];
        resp = state.responseQueue.toArray(resp);
        session.sendObjectsToClient( resp );
        session.sendObjectsToServer( req );
        return;
    }

    // private methods --------------------------------------------------------

    @SuppressWarnings("fallthrough")
    private void doHandleClientToken( NodeTCPSession session, Token token ) throws TokenException
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
                Header h = (Header)token;
                state.requestPersistent = isPersistent(h);
                Mode preMode = state.requestMode;
                h = doRequestHeader( session, h );

                String host = h.getValue("host");
                state.hosts.put( state.requestLineToken, host );

                /**
                 * Attach metadata
                 */
                session.globalAttach( NodeSession.KEY_HTTP_HOSTNAME, host );
                String uri = getRequestLine( session ).getRequestUri().normalize().getPath();
                session.globalAttach( NodeSession.KEY_HTTP_URI, uri );
                session.globalAttach( NodeSession.KEY_HTTP_URL, host + uri );

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
                Chunk c = (Chunk) token;
                Mode preMode = state.requestMode;
                c = doRequestBody( session, c );

                switch ( state.requestMode ) {
                case QUEUEING:
                    if ( c != null && c != Chunk.EMPTY ) {
                        state.requestQueue.add(c);
                    }
                    return;

                case RELEASED:
                    if ( c != null  && c != Chunk.EMPTY ) {
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
                    logger.error("queueing after EndMarker, release request");
                    releaseRequest( session );
                    /* fall through */

                case RELEASED:
                    doRequestBodyEnd( session );

                    state.requestQueue.add(EndMarker.MARKER);
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

    @SuppressWarnings("fallthrough")
    private void doHandleServerToken( NodeTCPSession session, Token token ) throws TokenException
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
                    if ( sc / 100 != 4 ) {
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
                if ( state.responseQueue.size() == 0 )
                    logger.warn("Invalid respones queue size.");

                session.sendObjectToClient( state.statusLine );
                return;

            case BLOCKED:
                if ( state.responseQueue.size() == 0 )
                    logger.warn("Invalid respones queue size.");

                session.sendObjectsToClient( state.responseResponse );
                state.responseResponse = null;
                return;

            default:
                throw new IllegalStateException("programmer malfunction");
            }

        case RESP_HEADER_STATE:
            if ( state.responseMode != Mode.BLOCKED ) {
                Header h = (Header)token;
                state.responsePersistent = isPersistent(h);

                /**
                 * Attach metadata
                 */
                String contentType = h.getValue("content-type");
                if (contentType != null) {
                    session.globalAttach( NodeSession.KEY_HTTP_CONTENT_TYPE, contentType );
                }
                String contentLength = h.getValue("content-length");
                if (contentLength != null) {
                    try {
                        Long contentLengthLong = Long.parseLong(contentLength);
                        session.globalAttach(NodeSession.KEY_HTTP_CONTENT_LENGTH, contentLengthLong );
                    } catch (NumberFormatException e) { /* ignore it if it doesnt parse */ }
                }

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
                Chunk c = (Chunk)token;
                c = doResponseBody( session, c );

                switch ( state.responseMode ) {
                case QUEUEING:
                    if (null != c && Chunk.EMPTY != c) {
                        state.responseQueue.add(c);
                    }
                    return;

                case RELEASED:
                    if (null != c && Chunk.EMPTY != c) {
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
                EndMarker em = (EndMarker)token;

                doResponseBodyEnd( session );
                if ( state.statusLine.getStatusCode() != 100 ) {
                    state.hosts.remove( state.responseRequest );
                }

                switch ( state.responseMode ) {
                case QUEUEING:
                    logger.warn("queueing after EndMarker, release repsonse");
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

    private ClientState nextClientState( ClientState clientState, Object o )
    {
        switch ( clientState ) {
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

    private ServerState nextServerState( ServerState serverState, Object o)
    {
        switch ( serverState ) {
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
