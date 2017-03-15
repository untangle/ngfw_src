/**
 * $Id$
 */
package com.untangle.node.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.EndMarkerToken;
import com.untangle.node.http.HeaderToken;
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

    protected HttpEventHandler()
    {
        super();
    }

    protected abstract RequestLineToken doRequestLine( AppTCPSession session, RequestLineToken rl );
    protected abstract HeaderToken doRequestHeader( AppTCPSession session, HeaderToken h );
    protected abstract ChunkToken doRequestBody( AppTCPSession session, ChunkToken c );
    protected abstract void doRequestBodyEnd( AppTCPSession session );

    protected abstract StatusLine doStatusLine( AppTCPSession session, StatusLine sl );
    protected abstract HeaderToken doResponseHeader( AppTCPSession session, HeaderToken h );
    protected abstract ChunkToken doResponseBody( AppTCPSession session, ChunkToken c );
    protected abstract void doResponseBodyEnd( AppTCPSession session );

    protected ClientState getClientState( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.clientState;
    }

    protected ServerState getServerState( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.serverState;
    }

    protected RequestLineToken getRequestLine( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.requestLineToken;
    }

    protected RequestLineToken getResponseRequest( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.responseRequest;
    }

    protected StatusLine getStatusLine( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.statusLine;
    }

    protected String getResponseHost( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.hosts.get( state.responseRequest );
    }

    protected boolean isRequestPersistent( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.requestPersistent;
    }

    protected boolean isResponsePersistent( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.responsePersistent;
    }

    protected Mode getRequestMode( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.requestMode;
    }

    protected Mode getResponseMode( AppTCPSession session )
    {
        HttpSessionState state = (HttpSessionState) session.attachment( SESSION_STATE_KEY );
        return state.responseMode;
    }

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

    protected void streamClient( AppTCPSession session, TokenStreamer streamer )
    {
        stream( session, AppSession.CLIENT, streamer );
    }

    protected void streamServer( AppTCPSession session, TokenStreamer streamer )
    {
        stream( session, AppSession.SERVER, streamer );
    }

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

    @Override
    public void handleTCPNewSession( AppTCPSession session )
    {
        HttpSessionState state = new HttpSessionState();
        session.attach( SESSION_STATE_KEY, state );
    }

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

    // private methods --------------------------------------------------------

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

            String rmethod = state.requestLineToken.getMethod().toString();
            if (rmethod != null)
                session.globalAttach(AppSession.KEY_HTTP_REQUEST_METHOD, rmethod);
            
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

                /**
                 * Attach metadata
                 */
                String host = h.getValue("host");
                String referer = h.getValue("referer");
                String uri = getRequestLine( session ).getRequestUri().normalize().getPath();
                session.globalAttach( AppSession.KEY_HTTP_HOSTNAME, host );
                session.globalAttach( AppSession.KEY_HTTP_REFERER, referer );
                session.globalAttach( AppSession.KEY_HTTP_URI, uri );
                session.globalAttach( AppSession.KEY_HTTP_URL, host + uri );

                String fpath = null;
                String fname = null;
                String fext = null;
                int loc;
                try {
                    // extract the full file path ignoring all params
                    fpath = (new URI(uri)).toString();

                    // find the last slash to extract the file name
                    loc = fpath.lastIndexOf("/");
                    if (loc != -1) fname = fpath.substring(loc + 1);

                    // find the last dot to extract the file extension
                    loc = fname.lastIndexOf(".");
                    if (loc != -1) fext = fname.substring(loc + 1);

                    if (fpath != null) session.globalAttach(AppSession.KEY_HTTP_REQUEST_FILE_PATH, fpath);
                    if (fname != null) session.globalAttach(AppSession.KEY_HTTP_REQUEST_FILE_NAME, fname);
                    if (fext != null) session.globalAttach(AppSession.KEY_HTTP_REQUEST_FILE_EXTENSION, fext);
                } catch (URISyntaxException e) {
                }

                
                h = doRequestHeader( session, h );

                host = h.getValue("host");
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

                /**
                 * Attach metadata
                 */
                String contentType = h.getValue("content-type");
                if (contentType != null) {
                    session.globalAttach( AppSession.KEY_HTTP_CONTENT_TYPE, contentType );
                }
                String contentLength = h.getValue("content-length");
                if (contentLength != null) {
                    try {
                        Long contentLengthLong = Long.parseLong(contentLength);
                        session.globalAttach(AppSession.KEY_HTTP_CONTENT_LENGTH, contentLengthLong );
                    } catch (NumberFormatException e) { /* ignore it if it doesnt parse */ }
                }
                String fileName = findContentDispositionFilename(h);
                if (fileName != null) {
                    session.globalAttach(AppSession.KEY_HTTP_RESPONSE_FILE_NAME, fileName);
                    // find the last dot to extract the file extension
                    int loc = fileName.lastIndexOf(".");
                    if (loc != -1)
                        session.globalAttach(AppSession.KEY_HTTP_RESPONSE_FILE_EXTENSION, fileName.substring(loc + 1));

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

    private boolean isPersistent(HeaderToken header)
    {
        String con = header.getValue("connection");
        return null == con ? false : con.equalsIgnoreCase("keep-alive");
    }

    public static String findContentDispositionFilename( HeaderToken header )
    {
        String contentDisposition = header.getValue("content-disposition");

        if ( contentDisposition == null )
            return null;

        contentDisposition = contentDisposition.toLowerCase();
        
        int indexOf = contentDisposition.indexOf("filename=");

        if ( indexOf == -1 )
            return null;

        indexOf = indexOf + "filename=".length();
        
        String filename = contentDisposition.substring( indexOf );
        
        filename = filename.replace("\"","");
        filename = filename.replace("'","");
        filename = filename.replaceAll("\\s","");
        
        return filename;
    }

}
