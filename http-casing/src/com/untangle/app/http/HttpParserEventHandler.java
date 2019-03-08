/**
 * $Id$
 */
package com.untangle.app.http;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTableEntry;
import com.untangle.uvm.DeviceTableEntry;
import com.untangle.uvm.util.AsciiCharBuffer;
import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.EndMarkerToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.TokenStreamer;
import com.untangle.uvm.vnet.ReleaseToken;
import com.untangle.uvm.app.MimeType;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.app.http.HeaderToken;

/**
 * An HTTP <code>Parser</code>.
 * The HTTP Parser takes raw data stream (ByteBuffers) as input and outputs HTTP Tokens
 */
public class HttpParserEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(HttpParserEventHandler.class);
    private static final String STATE_KEY = "http-parser-state";

    private static final byte SP = ' ';
    private static final byte HT = '\t';
    private static final byte CR = '\r';
    private static final byte LF = '\n';

    private static final int TIMEOUT = 30000;

    private static enum BodyEncoding {
        NO_BODY, // no body
        CLOSE_ENCODING, // server will close session after body
        CHUNKED_ENCODING, // chunked encoding
        CONTENT_LENGTH_ENCODING, // body will be specified length
    };
    
    private static enum ParseState {
        PRE_FIRST_LINE_STATE,
            FIRST_LINE_STATE,
            ACCUMULATE_HEADER_STATE,
            HEADER_STATE,
            CLOSED_BODY_STATE,
            CONTENT_LENGTH_BODY_STATE,
            CHUNK_LENGTH_STATE,
            CHUNK_BODY_STATE ,
            CHUNK_END_STATE ,
            LAST_CHUNK_STATE,
            END_MARKER_STATE,
            };
    
    private final HttpImpl app;
    
    private int maxHeader;
    private boolean blockLongHeaders;
    private int maxUri;
    private int maxRequestLine;
    private boolean blockLongUris;

    private boolean clientSide;
    
    /**
     * HttpParserSessionState stores all the parser state for the session
     */
    private class HttpParserSessionState
    {
        protected RequestLineToken requestLineToken;
        protected StatusLine statusLine;
        protected HeaderToken header;

        protected byte[] buf;
        protected ParseState currentState;
        protected BodyEncoding transferEncoding;
        protected long contentLength; /* counts down content-length and chunks */
        protected long lengthCounter; /* counts up to final */
    }

    /**
     * Create an HttpParserEventHandler.
     * @param clientSide - true if this is the client side
     * @param app - the HTTP app (casing)
     */
    protected HttpParserEventHandler( boolean clientSide, HttpImpl app )
    {
        this.clientSide = clientSide;
        this.app = app;
    }

    /**
     * handleTCPNewSession.
     * @param session
     */
    @Override
    public void handleTCPNewSession( AppTCPSession session )
    {
        /**
         * FIXME move this somewhere else
         * cant put it in initializer because settings aren't read yet
         */
        HttpSettings settings = app.getHttpSettings();
        this.maxHeader = settings.getMaxHeaderLength();
        this.blockLongHeaders = settings.getBlockLongHeaders();
        this.maxUri = settings.getMaxUriLength();
        this.maxRequestLine = maxUri + 13;
        this.blockLongUris = settings.getBlockLongUris();

        HttpParserSessionState state = new HttpParserSessionState();
        state.buf = null;
        state.currentState = ParseState.PRE_FIRST_LINE_STATE;
        session.attach( STATE_KEY, state );

        lineBuffering( session, true );
    }
    
    /**
     * handleTCPClientChunk.
     * @param session
     * @param data
     */
    @Override
    public void handleTCPClientChunk( AppTCPSession session, ByteBuffer data )
    {
        if (clientSide) {
            parse( session, data, false, false );
        } else {
            logger.warn("Received data when expect object");
            throw new RuntimeException("Received data when expect object");
        }
    }

    /**
     * handleTCPServerChunk.
     * @param session
     * @param data
     */
    @Override
    public void handleTCPServerChunk( AppTCPSession session, ByteBuffer data )
    {
        if (clientSide) {
            logger.warn("Received data when expect object");
            throw new RuntimeException("Received data when expect object");
        } else {
            parse( session, data, true, false );
            return;
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
        if ( obj instanceof ReleaseToken ) {
            session.sendObjectToServer( obj );
            session.release();
            return;
        }

        logger.warn("Received object but expected data.");
        throw new RuntimeException("Received object but expected data.");
    }
    
    /**
     * handleTCPServerObject.
     * @param session
     * @param obj
     */
    @Override
    public void handleTCPServerObject( AppTCPSession session, Object obj )
    {
        if ( obj instanceof ReleaseToken ) {
            session.sendObjectToClient( obj );
            session.release();
            return;
        }
        
        logger.warn("Received object but expected data.");
        throw new RuntimeException("Received object but expected data.");
    }
    
    /**
     * handleTCPClientDataEnd.
     * @param session
     * @param data
     */
    @Override
    public void handleTCPClientDataEnd( AppTCPSession session, ByteBuffer data )
    {
        if (clientSide) {
            parse( session, data, false, true);
            return;
        } else {
            if ( data.hasRemaining() ) {
                logger.warn("Received data when expect object");
                throw new RuntimeException("Received data when expect object");
            }
            return;
        }
    }

    /**
     * handleTCPServerDataEnd.
     * @param session
     * @param data
     */
    @Override
    public void handleTCPServerDataEnd( AppTCPSession session, ByteBuffer data )
    {
        if (clientSide) {
            if ( data.hasRemaining() ) {
                logger.warn("Received data when expect object");
                throw new RuntimeException("Received data when expect object");
            }
        } else {
            parse( session, data, true, true );
            return;
        }
    }

    /**
     * handleTCPClientFIN.
     * @param session
     */
    @Override
    public void handleTCPClientFIN( AppTCPSession session )
    {
        if (clientSide) {
            endSession( session );
        } else {
            logger.warn("Received unexpected event.");
            throw new RuntimeException("Received unexpected event.");
        }

        return;
    }

    /**
     * handleTCPServerFIN.
     * @param session
     */
    @Override
    public void handleTCPServerFIN( AppTCPSession session )
    {
        if (clientSide) {
            logger.warn("Received unexpected event.");
            throw new RuntimeException("Received unexpected event.");
        } else {
            endSession( session );
        }

        return;
    }
    
    /**
     * Parse the actual data, and send equivalent tokens in the session
     * @param session - the session
     * @param data - the data to be parsed
     * @param s2c - true if server-to-client, false if client-to-server
     * @param last - true if last data
     */
    private void parse( AppTCPSession session, ByteBuffer data, boolean s2c, boolean last )
    {
        ByteBuffer buf = data;
        ByteBuffer dup = buf.duplicate();
        try {
            parse( session, buf, last );
        } catch (Throwable exn) {
            String sessionEndpoints = "[" +
                session.getProtocol() + " : " + 
                session.getClientAddr().getHostAddress() + ":" + session.getClientPort() + " -> " +
                session.getServerAddr().getHostAddress() + ":" + session.getServerPort() + "]";

            /**
             * Some Special handling for semi-common parse exceptions
             * Otherwise just print the full stack trace
             *
             * Parse exception are quite common in the real world as people use non-compliant
             * or different protocol on standard ports.
             * As such we don't want to litter the logs too much with these warnings, but we don't want to eliminate
             * them entirely.
             */
            String message = exn.getMessage();
            if (message != null && message.contains("no digits found")) {
                logger.info("Protocol parse exception (no digits found). Releasing session: " + sessionEndpoints);
            } else if (message != null && message.contains("expected")) {
                logger.info("Protocol parse exception (got != expected). Releasing session: " + sessionEndpoints);
            } else if (message != null && message.contains("data trapped")) {
                logger.info("Protocol parse exception (data trapped). Releasing session: " + sessionEndpoints);
                // "data trapped" means that we've already buffered data, and have now discovered its probably not
                // a protocol we can understand.
                // Since we've already buffered data we need to reset the bytebuffer to send the data we've already buffered
                // to do se reset the position to zero, and the the limit to the current position.
                // Bug #11886 for more details
                dup.limit(dup.position());
                dup.position(0);
            } else if (message != null && message.contains("buf limit exceeded")) {
                logger.info("Protocol parse exception (buf limit exceeded). Releasing session: " + sessionEndpoints);
            } else if (message != null && message.contains("header exceeds")) {
                logger.info("Protocol parse exception (header exceeds). Releasing session: " + sessionEndpoints);
            } else if (message != null && message.contains("length exceeded")) {
                logger.info("Protocol parse exception (request length exceeded). Releasing session: " + sessionEndpoints);
            } else if (message != null && message.contains("invalid method")) {
                logger.info("Protocol parse exception (invalid request method). Releasing session: " + sessionEndpoints);                    
            } else {
                logger.info("Protocol parse exception. releasing session: " + sessionEndpoints, exn);
            }
                
            session.release();

            if ( s2c ) {
                session.sendObjectToClient( new ReleaseToken() );
                session.sendDataToClient( dup );
            } else {
                session.sendObjectToServer( new ReleaseToken() );
                session.sendDataToServer( dup );
            }
            return;
        }
    }
    
    /**
     * Parse the actual data, and send equivalent tokens in the session
     * @param session - the session
     * @param buffer - the data to be parsed
     * @param last - true if last data
     */
    public void parse( AppTCPSession session, ByteBuffer buffer, boolean last )
    {
        HttpParserSessionState state = (HttpParserSessionState) session.attachment( STATE_KEY );
        cancelTimer( session );

        // if the session has ended
        if ( last ) {
            // if there is data throw a warning
            // except for ACCUMULATE_HEADER_STATE AND CLOSED_BODY_STATE
            // both of which can have the connection closed while data is still in buffer
            if ( buffer.hasRemaining() &&
                 state.currentState != ParseState.ACCUMULATE_HEADER_STATE &&
                 state.currentState != ParseState.CLOSED_BODY_STATE ) {
                throw new RuntimeException("in state: " + state + " data trapped in read buffer: " + buffer.remaining());
            }
            // if the connection is closed and there is no data to parse just return
            // except for CLOSED_BODY_STATE, which we need to handle because the close of the connection
            // signals the end of the body
            if ( !buffer.hasRemaining() &&
                 state.currentState != ParseState.CLOSED_BODY_STATE ) {
                return;
            }
        }
             
        if (logger.isDebugEnabled()) {
            logger.debug("parsing chunk: " + buffer);
        }
        List<Token> tokenList = new LinkedList<Token>();

        boolean done = false;
        while (!done) {
            if (logger.isDebugEnabled()) {
                logger.debug("HTTP state: " + state.currentState + " data: " + buffer + ( clientSide ? " from client" : " from server" ));
            }

            switch ( state.currentState ) {
            case PRE_FIRST_LINE_STATE: {
                state.lengthCounter = 0;
                    
                // Once we have three bytes we look to see if we are on the
                // client side and working with a valid request method.
                // See bug #11886 for more details 
                if ((clientSide) && (buffer.limit() >= 3)) {
                    String leader = new String(buffer.array(),0,3);

                    switch(leader) {
                    case "GET":     // GET
                    case "HEA":     // HEAD
                    case "POS":     // POST
                    case "OPT":     // OPTIONS
                    case "PUT":     // PUT
                    case "DEL":     // DELETE
                    case "TRA":     // TRACE
                    case "CON":     // CONNECT
                    case "NON":     // NON-STANDARD
                        // these look like valid HTTP requests
                        break;

                    default:
                        // this does not look like HTTP
                        throw new RuntimeException("HTTP request invalid method: " + AsciiCharBuffer.wrap(buffer));                            
                    }
                }

                if (buffer.hasRemaining() && completeLine(buffer)) {
                    ByteBuffer d = buffer.duplicate();
                    byte b1 = d.get();
                    if ( LF == b1 || d.hasRemaining() && CR == b1 && LF == d.get() ) {
                        buffer = null;
                        done = true;
                    } else {
                        state.currentState = ParseState.FIRST_LINE_STATE;
                    }
                } else if (buffer.remaining() > maxRequestLine) {
                    throw new RuntimeException("HTTP request length exceeded: " + AsciiCharBuffer.wrap(buffer));
                } else {
                    if (buffer.capacity() < maxRequestLine) {
                        ByteBuffer r = ByteBuffer.allocate(maxRequestLine);
                        r.put(buffer);
                        buffer = r;
                    } else {
                        buffer.compact();
                    }
                    done = true;
                }

                break;
            }
            case FIRST_LINE_STATE: {
                // Initialize the buffer, we'll need it until
                // we're done with HEADER state.
                state.buf = new byte[maxUri];

                if (completeLine(buffer)) {
                    tokenList.add( firstLine( session, buffer ) );

                    state.currentState = ParseState.ACCUMULATE_HEADER_STATE;
                } else {
                    buffer.compact();
                    done = true;
                }
                break;
            }
            case ACCUMULATE_HEADER_STATE: {
                // if session has ended, send header on 
                if ( last ) {
                    buffer.flip();
                    if ( clientSide ) {
                        session.sendObjectToServer( header( session, buffer) );
                    } else {
                        session.sendObjectToClient( header( session, buffer) );
                    }
                    return;
                }                    

                if (!completeHeader(buffer)) {
                    if (buffer.capacity() < maxHeader) {
                        ByteBuffer nb = ByteBuffer.allocate(maxHeader + 2);
                        nb.put(buffer);
                        nb.flip();
                        buffer = nb;
                    } else if (buffer.remaining() >= maxHeader) {
                        String msg = "header exceeds " + maxHeader + ":\n" + AsciiCharBuffer.wrap(buffer);
                        if (blockLongHeaders) {
                            logger.warn(msg);
                            // XXX send error page instead
                            session.shutdownClient();
                            session.shutdownServer();
                            return;
                        } else {
                            // allow session to be released, or not
                            throw new RuntimeException(msg);
                        }
                    }

                    buffer.compact();

                    done = true;
                } else {
                    state.currentState = ParseState.HEADER_STATE;
                }
                break;
            }
            case HEADER_STATE: {
                state.header = header( session, buffer );
                // queue the header to be sent
                tokenList.add( state.header );

                // Done with buf now
                state.buf = null;

                if ( buffer.hasRemaining() )
                    logger.warn("bytes remaining in buffer");

                if (!clientSide) {
                    if ( state.requestLineToken != null ) {
                        HttpMethod method = state.requestLineToken.getMethod();
                        if ( method == HttpMethod.HEAD ) {
                            state.transferEncoding = BodyEncoding.NO_BODY;
                        }
                    }
                    String contentType = state.header.getValue("content-type");
                    String contentLength = state.header.getValue("content-length");
                    String fileName = findContentDispositionFilename(state.header);

                    /**
                     * Attach values to session
                     */
                    if (contentType != null) {
                        session.globalAttach( AppSession.KEY_HTTP_CONTENT_TYPE, contentType );
                    }
                    if (contentLength != null) {
                        try {
                            Long contentLengthLong = Long.parseLong(contentLength);
                            session.globalAttach(AppSession.KEY_HTTP_CONTENT_LENGTH, contentLengthLong );
                        } catch (NumberFormatException e) { /* ignore it if it doesnt parse */ }
                    }
                    if (fileName != null) {
                        session.globalAttach(AppSession.KEY_HTTP_RESPONSE_FILE_NAME, fileName);
                        // find the last dot to extract the file extension
                        int loc = fileName.lastIndexOf(".");
                        if (loc != -1)
                            session.globalAttach(AppSession.KEY_HTTP_RESPONSE_FILE_EXTENSION, fileName.substring(loc + 1));
                    }

                } else {
                    /* the request event is saved internally and used later with getRequestEvent */
                    String referer = ( app.getHttpSettings().getLogReferer() ? state.header.getValue("referer") : null);
                    HttpRequestEvent evt = new HttpRequestEvent( state.requestLineToken.getRequestLine(), state.header.getValue("host"), referer, state.lengthCounter );
                    state.requestLineToken.getRequestLine().setHttpRequestEvent(evt);
                }

                if ( state.transferEncoding == BodyEncoding.NO_BODY ) {
                    state.currentState = ParseState.END_MARKER_STATE;
                    done = false;
                } else if ( state.transferEncoding == BodyEncoding.CLOSE_ENCODING ) {
                    lineBuffering( session, false );
                    state.currentState = ParseState.CLOSED_BODY_STATE;
                    done = true;
                    buffer = null;
                } else if ( state.transferEncoding == BodyEncoding.CHUNKED_ENCODING ) {
                    lineBuffering( session, true );
                    state.currentState = ParseState.CHUNK_LENGTH_STATE;
                    done = true;
                    buffer = null;
                } else if ( state.transferEncoding == BodyEncoding.CONTENT_LENGTH_ENCODING ) {
                    lineBuffering( session, false );
                    if ( buffer.hasRemaining() )
                        logger.warn("bytes remaining in buffer");
                        
                    if ( state.contentLength > 0 ) {
                        readLimit( session, state.contentLength );
                        state.currentState = ParseState.CONTENT_LENGTH_BODY_STATE;
                        done = true;
                        buffer = null;
                    } else {
                        state.currentState = ParseState.END_MARKER_STATE;
                    }
                } else {
                    logger.warn("Invalid transferEncoding: " + state.transferEncoding);
                }
                
                break;
            }
            case CLOSED_BODY_STATE: {
                tokenList.add( closedBody( session, buffer ) );
                if ( last ) {
                    buffer = null;
                    state.currentState = ParseState.END_MARKER_STATE;
                } else {
                    buffer = null;
                    done = true;
                }
                break;
            }
            case CONTENT_LENGTH_BODY_STATE: {
                tokenList.add( chunk( session, buffer ) );
                if ( state.contentLength == 0 ) {
                    buffer = null;
                    // XXX handle trailer
                    state.currentState = ParseState.END_MARKER_STATE;
                } else {
                    readLimit( session, state.contentLength );
                    buffer = null;
                    done = true;
                }
                break;
            }
            case CHUNK_LENGTH_STATE: {
                // chunk-size     = 1*HEX
                if (!completeLine(buffer)) {
                    buffer.compact();
                    done = true;
                    break;
                }

                state.contentLength = chunkLength(buffer);
                if ( state.contentLength == 0 ) {
                    buffer = null;
                    state.currentState = ParseState.LAST_CHUNK_STATE;
                } else {
                    lineBuffering( session, false );
                    if ( buffer.hasRemaining() )
                        logger.warn("bytes remaining in buffer");

                    readLimit( session, state.contentLength );
                    buffer = null;

                    state.currentState = ParseState.CHUNK_BODY_STATE;
                }
                done = true;
                break;
            }
            case CHUNK_BODY_STATE: {
                tokenList.add( chunk( session, buffer ) );

                if ( state.contentLength == 0 ) {
                    lineBuffering( session, true );
                    buffer = null;
                    state.currentState = ParseState.CHUNK_END_STATE;
                } else {
                    readLimit( session, state.contentLength );
                    buffer = null;
                }

                done = true;
                break;
            }
            case CHUNK_END_STATE: {
                if (!completeLine(buffer)) {
                    buffer.compact();
                    done = true;
                    break;
                }

                eatCrLf(buffer);
                if ( buffer.hasRemaining() )
                    logger.warn("bytes remaining in buffer");

                buffer = null;
                done = true;

                state.currentState = ParseState.CHUNK_LENGTH_STATE;
                break;
            }
            case LAST_CHUNK_STATE: {
                // last-chunk     = 1*("0") [ chunk-extension ] CRLF
                if (!completeLine(buffer)) {
                    buffer.compact();
                    done = true;
                    break;
                }

                eatCrLf(buffer);

                if ( buffer.hasRemaining() )
                    logger.warn("bytes remaining in buffer");

                buffer = null;

                state.currentState = ParseState.END_MARKER_STATE;
                break;
            }
            case END_MARKER_STATE: {
                EndMarkerToken endMarker = EndMarkerToken.MARKER;
                tokenList.add(endMarker);
                lineBuffering( session, true );
                buffer = null;
                state.currentState = ParseState.PRE_FIRST_LINE_STATE;

                if (!clientSide) {
                    String contentType = state.header.getValue("content-type");
                    String mimeType = null == contentType ? null : MimeType.getType(contentType);
                    RequestLine rl = null == state.requestLineToken ? null : state.requestLineToken.getRequestLine();
                    String filename = findContentDispositionFilename(state.header);
                    
                    if (null != rl) {
                        HttpResponseEvent evt = new HttpResponseEvent(rl, mimeType, filename, state.lengthCounter);

                        app.logEvent(evt);
                    }
                } else {
                    HttpRequestEvent evt = state.requestLineToken.getRequestLine().getHttpRequestEvent();
                    evt.setContentLength( state.lengthCounter );

                    if (evt.getRequestUri() == null) {
                        logger.warn("null request for: " + session.sessionEvent());
                    }

                    app.logEvent(evt);

                    /**
                     * Update host table with header info
                     * if an entry already exists for this host
                     */
                    InetAddress clientAddr = session.sessionEvent().getCClientAddr();
                    String agentString = state.header.getValue("user-agent");
                    String host = state.header.getValue("host");
                    String referer = state.header.getValue("referer");
                    String userAgent = state.header.getValue("user-agent");
                    String rmethod = state.requestLineToken.getMethod().toString();
                    /**
                     * XXX what is this: .replaceAll("(?<!:)/+", "/")
                     * -dmorris
                     */
                    String uri = state.requestLineToken.getRequestLine().getRequestUri().normalize().toString().replaceAll("(?<!:)/+", "/");
                    String path = state.requestLineToken.getRequestLine().getRequestUri().normalize().getPath();
                    HostTableEntry hostEntry = null;
                    DeviceTableEntry deviceEntry = null;
                    if ( clientAddr != null )
                        hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( clientAddr );
                    if ( hostEntry != null )
                        deviceEntry = UvmContextFactory.context().deviceTable().getDevice( hostEntry.getMacAddress() );

                    session.globalAttach( AppSession.KEY_HTTP_HOSTNAME, host );
                    session.globalAttach( AppSession.KEY_HTTP_REFERER, referer );
                    session.globalAttach( AppSession.KEY_HTTP_URI, uri );
                    session.globalAttach( AppSession.KEY_HTTP_URL, host + uri );
                    session.globalAttach( AppSession.KEY_HTTP_USER_AGENT, userAgent );
                    session.globalAttach( AppSession.KEY_HTTP_REQUEST_METHOD, rmethod );

                    String fpath = null;
                    String fname = null;
                    String fext = null;
                    int loc;
                    try {
                        // extract the full file path ignoring all params
                        fpath = (new URI(path)).toString();
                        fname = fpath;

                        // find the last slash to extract the file name
                        loc = fpath.lastIndexOf("/");
                        if (loc != -1) fname = fpath.substring(loc + 1);
                        else fname = fpath;

                        // find the last dot to extract the file extension
                        loc = fname.lastIndexOf(".");
                        if (loc != -1) fext = fname.substring(loc + 1);

                        if (fpath != null) session.globalAttach(AppSession.KEY_HTTP_REQUEST_FILE_PATH, fpath);
                        // FILE_NAME can be set from earlier in the header, only overwrite if it is null
                        if (fname != null && session.globalAttachment(AppSession.KEY_HTTP_REQUEST_FILE_NAME) == null) {
                            session.globalAttach(AppSession.KEY_HTTP_REQUEST_FILE_NAME, fname);
                        }
                        // FILE_EXTENSION can be set from earlier in the header, only overwrite if it is null
                        if (fext != null && session.globalAttachment(AppSession.KEY_HTTP_REQUEST_FILE_EXTENSION) == null)
                            session.globalAttach(AppSession.KEY_HTTP_REQUEST_FILE_EXTENSION, fext);
                    } catch (URISyntaxException e) {}

                    if ( agentString != null ) {
                        if ( hostEntry != null )
                            hostEntry.setHttpUserAgent( agentString );
                        if ( deviceEntry != null )
                            deviceEntry.setHttpUserAgent( agentString );
                    }
                }

                // Free up header storage
                state.header = null;
                done = true;
                break;
            }
            default:
                logger.warn("Invalid currentState: " + state.currentState);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("remaining readBuffer: " + buffer);
        }

        scheduleTimer( session, TIMEOUT );

        if ( buffer != null && !buffer.hasRemaining() ) {
            String msg = "buffer does not have remaining: " + buffer + " in state: " + state.currentState;
            buffer.flip();
            msg += " buffer contents: '" + AsciiCharBuffer.wrap(buffer) + "'";
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        if ( clientSide ) {
            session.setClientBuffer( buffer );
            for ( Token token : tokenList ) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Sending " + token + " to server.");
                }
                session.sendObjectToServer( token );
            }
        } else {
            session.setServerBuffer( buffer );
            for ( Token token : tokenList ) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Sending " + token + " to client.");
                }
                session.sendObjectToClient( token );
            }
        }
    }

    /**
     * completeLine.
     * @param buffer
     * @return
     */
    private boolean completeLine(ByteBuffer buffer)
    {
        return buffer.get(buffer.limit() - 1) == LF;
    }

    /**
     * completeHeader.
     * @param buffer
     * @return
     */
    private boolean completeHeader(ByteBuffer buffer)
    {
        ByteBuffer d = buffer.duplicate();

        // no header
        if (d.remaining() > 0 && d.remaining() <= 2) {
            if (LF == d.get(d.limit() - 1)) {
                return true;
            }
        }

        if (d.remaining() >= 4) {
            d.position(d.limit() - 4);
        }

        byte c = ' ';
        while (CR != c && LF != c) {
            if (d.hasRemaining()) {
                c = d.get();
            } else {
                return false;
            }
        }

        if (LF == c || CR == c && d.hasRemaining() && LF == d.get()) {
            if (d.hasRemaining()) {
                c = d.get();
                return LF == c || CR == c && d.hasRemaining() && LF == d.get();
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * endSession.
     * @param session
     */
    public void endSession( AppTCPSession session )
    {
        HttpParserSessionState state = (HttpParserSessionState) session.attachment( STATE_KEY );

        switch ( state.currentState ) {
        case PRE_FIRST_LINE_STATE:
            break;
        case ACCUMULATE_HEADER_STATE:
            logger.warn("endSession in ACCUMULATE_HEADER_STATE");
            break;
        case HEADER_STATE:
            logger.warn("endSession in HEADER_STATE");
            break;
        case CONTENT_LENGTH_BODY_STATE:
            logger.warn("endSession in CONTENT_LENGTH_BODY_STATE, length: " + state.contentLength);
            endMarkerStreamer();
            break;
        case CHUNK_LENGTH_STATE:
            logger.warn("endSession in CHUNK_LENGTH_STATE");
            endMarkerStreamer();
            break;
        case CHUNK_BODY_STATE:
            logger.warn("endSession in CHUNK_BODY_STATE, length: " + state.contentLength);
            endMarkerStreamer();
            break;
        case CHUNK_END_STATE:
            logger.warn("endSession in CHUNK_END_STATE");
            endMarkerStreamer();
            break;
        case LAST_CHUNK_STATE:
            logger.warn("endSession in LAST_CHUNK_STATE");
            endMarkerStreamer();
            break;
        case END_MARKER_STATE:
            logger.warn("endSession in END_MARKER_STATE");
            endMarkerStreamer();
            break;
        case CLOSED_BODY_STATE:
            /* this case is legit */
            endMarkerStreamer();
            break;
        default:
            logger.warn("endSession unhandled state: " + state.currentState);
            break;
        }

        if ( clientSide )
            session.shutdownServer();
        else
            session.shutdownClient();
        return;
    }

    /**
     * handleTimer.
     * @param sess
     */
    public void handleTimer( AppSession sess )
    {
        AppTCPSession session = (AppTCPSession) sess;
        byte cs = session.clientState();
        byte ss = session.serverState();

        if (logger.isDebugEnabled()) {
            logger.debug("handling timer cs=" + cs + " ss=" + ss);
        }

        if (cs == AppTCPSession.HALF_OPEN_OUTPUT
            && ss == AppTCPSession.HALF_OPEN_INPUT) {
            if (logger.isDebugEnabled()) {
                logger.debug("closing session in halfstate");
            }
            session.shutdownClient();
        } else {
            scheduleTimer( session, TIMEOUT );
        }
    }

    /**
     * firstLine.
     * @param session
     * @param data
     * @return
     */
    private Token firstLine( AppTCPSession session, ByteBuffer data )
    {
        HttpParserSessionState state = (HttpParserSessionState) session.attachment( STATE_KEY );

        if (!clientSide) {
            state.statusLine = statusLine( session, data );
            return state.statusLine;
        } else {
            state.requestLineToken = requestLine( session, data );
            return state.requestLineToken;
        }
    }

    /**
     * Request-Line   = Method SP Request-URI SP HTTP-Version CRLF
     * @param session
     * @param data
     * @return
     */
    private RequestLineToken requestLine( AppTCPSession session, ByteBuffer data )
    {
        HttpParserSessionState state = (HttpParserSessionState) session.attachment( STATE_KEY );

        state.transferEncoding = BodyEncoding.NO_BODY;

        HttpMethod method = HttpMethod.getInstance( token( session, data ) );
        if (logger.isDebugEnabled()) {
            logger.debug("method: " + method);
        }
        eat(data, SP);
        byte[] requestUri = requestUri( session, data );
        eat(data, SP);
        String httpVersion = version( data );
        eatCrLf(data);

        RequestLine rl = new RequestLine( session.sessionEvent(), method, requestUri );
        return new RequestLineToken( rl, httpVersion );
    }

    /**
     * Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
     * @param session
     * @param data
     * @return
     */
    private StatusLine statusLine( AppTCPSession session, ByteBuffer data )
    {
        HttpParserSessionState state = (HttpParserSessionState) session.attachment( STATE_KEY );

        state.transferEncoding = BodyEncoding.CLOSE_ENCODING;

        String httpVersion = version(data);
        eat(data, SP);
        int statusCode = statusCode(data);
        eat(data, SP);
        String reasonPhrase = reasonPhrase( state, data );
        eatCrLf(data);

        // 4.4 Message Length
        // 1. Any response message which "MUST NOT" include a
        // message-body (such as the 1xx, 204, and 304 responses and
        // any response to a HEAD request) is always terminated by the
        // first empty line after the header fields, regardless of the
        // entity-header fields present in the message.
        if ( statusCode >= 100 && statusCode <= 199 || statusCode == 204 || statusCode == 304 ) {
            state.transferEncoding = BodyEncoding.NO_BODY;
        }

        if (100 != statusCode && 408 != statusCode) {
            RequestLineToken rl = dequeueRequest( session, statusCode );
            // casing returns null and logs an error when nothing in queue
            state.requestLineToken = ( rl != null ? rl : state.requestLineToken );
        }

        return new StatusLine(httpVersion, statusCode, reasonPhrase);
    }

    /**
     * HTTP-Version   = "HTTP" "/" 1*DIGIT "." 1*DIGIT
     * @param data
     * @return
     */
    private String version(ByteBuffer data)
    {
        eat(data, "HTTP");
        eat(data, '/');
        int maj = eatDigits(data);
        eat(data, '.');
        int min = eatDigits(data);

        return "HTTP/" + maj + "." + min;
    }

    /**
     * Reason-Phrase  = *<TEXT, excluding CR, LF>
     * @param state
     * @param buffer
     * @return
     */
    private String reasonPhrase( HttpParserSessionState state, ByteBuffer buffer )
    {
        int l = buffer.remaining();

        for (int i = 0; buffer.hasRemaining(); i++) {
            if ( isCtl( state.buf[i] = buffer.get() ) ) {
                buffer.position(buffer.position() - 1);
                return new String(state.buf, 0, i);
            }
        }

        return new String(state.buf, 0, l);
    }

    /**
     * Status-Code    =
     *       "100"  ; Section 10.1.1: Continue
     *     | ...
     *     | extension-code
     * extension-code = 3DIGIT
     * @param buffer
     * @return
     */
    private int statusCode(ByteBuffer buffer)
    {
        int i = eatDigits(buffer);

        if (1000 < i || 100 > i) {
            // assumes no status codes begin with 0
            throw new RuntimeException("expected 3 DIGITs, got: " + i);
        }

        return i;
    }

    /**
     * header.
     * @param session
     * @param data
     * @return
     */
    private HeaderToken header( AppTCPSession session, ByteBuffer data )
    {
        HeaderToken header = new HeaderToken();

        while (data.remaining() > 2) {
            field( session, header, data );
            eatCrLf(data);
        }

        while (data.hasRemaining()) {
            eatCrLf(data);
        }

        return header;
    }

    /**
     * message-header = field-name ":" [ field-value ]
     * field-name     = token
     * field-value    = *( field-content | LWS )
     * field-content  = <the OCTETs making up the field-value
     *                  and consisting of either *TEXT or combinations
     *                  of token, separators, and quoted-string>
     * @param session
     * @param header
     * @param data
     */
    private void field( AppTCPSession session, HeaderToken header, ByteBuffer data )
    {
        HttpParserSessionState state = (HttpParserSessionState) session.attachment( STATE_KEY );

        String key = token( session, data ).trim();
        eat(data, ':');
        String value = eatText( session, data ).trim();

        // 4.3: The presence of a message-body in a request is signaled by the
        // inclusion of a Content-Length or Transfer-Encoding header field in
        // the request's message-headers.
        // XXX check for valid body in the *reply* as well!
        if (key.equalsIgnoreCase("transfer-encoding")) {
            if (value.equalsIgnoreCase("chunked")) {
                if (logger.isDebugEnabled()) {
                    logger.debug("using chunked encoding");
                }
                state.transferEncoding = BodyEncoding.CHUNKED_ENCODING;
            } else {
                logger.warn("don't know transfer-encoding: " + value);
            }
        } else if ( key.equalsIgnoreCase("content-length") && state.transferEncoding != BodyEncoding.CHUNKED_ENCODING ) {

            if (logger.isDebugEnabled()) {
                logger.debug("using content length encoding");
            }
            state.transferEncoding = BodyEncoding.CONTENT_LENGTH_ENCODING;
            state.contentLength = Long.parseLong(value);
            if (logger.isDebugEnabled()) {
                logger.debug("contentLength = " + state.contentLength);
            }
        } 

        // Some servers do not support 100-continue so clients should send the data anyway immediately (section 8.2.3)
        // However, some clients do not, and since we do not support a continue state and it complicates the logic, 
        // as a hack, we spoof a response to tell the client to continue immediately.
        if (key.equalsIgnoreCase("expect") && value.equalsIgnoreCase("100-continue")) {
            String response = state.requestLineToken.getHttpVersion() + " 100 Continue\r\n\r\n";
            if (logger.isDebugEnabled()) {
                logger.debug("Expect Header " + key + "=" + value);
                logger.debug("Spoofing Reply: " + response);
            }

            session.sendDataToClient( ByteBuffer.wrap( response.getBytes() ) );
            return; // don't add this one to the header so the server won't also respond with a continue
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Header " + key + "=" + value);
        }
        
        header.addField( key, value );
    }

    /**
     * closedBody.
     * @param session
     * @param buffer
     * @return
     */
    private ChunkToken closedBody( AppTCPSession session, ByteBuffer buffer )
    {
        HttpParserSessionState state = (HttpParserSessionState) session.attachment( STATE_KEY );

        state.lengthCounter += buffer.remaining();
        return new ChunkToken(buffer.slice());
    }

    /**
     * chunkLength.
     * @param buffer
     * @return
     */
    private int chunkLength(ByteBuffer buffer)
    {
        int i = 0;

        while (buffer.hasRemaining()) {
            byte c = buffer.get();
            if (isHex(c)) {
                i = 16 * i + hexValue((char)c);
            } else if (';' == c) {
                // XXX
                logger.warn("chunk extension not supported yet");
            } else if (CR == c || LF == c) {
                buffer.position(buffer.position() - 1);
                break;
            } else if (SP == c) {
                // ignore spaces
            } else {
                // XXX
                logger.warn("unknown character in chunk length: " + c);
            }
        }

        eatCrLf(buffer);

        return i;
    }

    /**
     * chunk          = chunk-size [ chunk-extension ] CRLF
     *                  chunk-data CRLF
     * @param session
     * @param buffer
     * @return
     */
    private ChunkToken chunk( AppTCPSession session, ByteBuffer buffer )
    {
        HttpParserSessionState state = (HttpParserSessionState) session.attachment( STATE_KEY );

        int remaining = buffer.remaining();
        state.contentLength -= remaining;
        state.lengthCounter += remaining;

        if ( state.contentLength < 0 )
            logger.warn("Invalid content lengeth");

        return new ChunkToken(buffer.slice());
    }

    /**
     * Request-URI    = "*" | absoluteURI | abs_path | authority
     * @param session
     * @param buffer
     * @return
     */
    private byte[] requestUri( AppTCPSession session, ByteBuffer buffer )
       
    {
        HttpParserSessionState state = (HttpParserSessionState) session.attachment( STATE_KEY );

        ByteBuffer dup = buffer.duplicate();

        for (int i = 0; buffer.hasRemaining(); i++) {
            if (maxUri <= i && blockLongUris) {
                String msg = "(buf limit exceeded) " + state.buf.length + ": " + new String(state.buf);
                session.shutdownClient();
                session.shutdownServer();
                throw new RuntimeException("blocking " + msg);
            }

            char c = (char)buffer.get();

            if (SP == c || HT == c) {
                buffer.position(buffer.position() - 1);
                dup.limit(buffer.position());
                break;
            }
        }

        byte[] a = new byte[dup.remaining()];
        dup.get(a);

        return a;
    }

    /**
     * eat.
     * @param data
     * @param s
     */
    private void eat( ByteBuffer data, String s )
    {
        byte[] sb = s.getBytes();
        for (int i = 0; i < sb.length; i++) {
            eat(data, sb[i]);
        }
    }

    /**
     * eat.
     * @param data
     * @param c
     * @return
     */
    private boolean eat(ByteBuffer data, char c)
    {
        return eat(data, (byte)c);
    }

    /**
     * eat.
     * @param data
     * @param c
     * @return
     */
    private boolean eat(ByteBuffer data, byte c)
    {
        if (!data.hasRemaining()) {
            return false;
        }

        int b = data.get();
        if (b != c) {
            logger.debug("expected " + b + " bytes, but got " + c + " bytes.");
            data.position(data.position() - 1);
            return false;
        } else {
            return true;
        }
    }

    /**
     * read *TEXT, folding LWS
     * TEXT           = <any OCTET except CTLs,
     *                   but including LWS>
     * @param session
     * @param buffer
     * @return string
     */
    private String eatText( AppTCPSession session, ByteBuffer buffer )
    {
        HttpParserSessionState state = (HttpParserSessionState) session.attachment( STATE_KEY );
        eatLws(buffer);

        int l = buffer.remaining();

        for (int i = 0; buffer.hasRemaining(); i++) {
            if ( state.buf.length <= i ) {
                String msg = "(buf limit exceeded) " + state.buf.length + ": " + new String(state.buf);
                if (blockLongUris) {
                    session.shutdownClient();
                    session.shutdownServer();
                    throw new RuntimeException("blocking " + msg);
                } else {
                    throw new RuntimeException("non-http " + msg);
                }
            }
            state.buf[i] = buffer.get();
            if ( isCtl( state.buf[i] ) ) {
                buffer.position(buffer.position() - 1);
                if (eatLws(buffer)) {
                    state.buf[i] = SP;
                } else {
                    byte b1 = buffer.get(buffer.position());
                    byte b2 = buffer.get(buffer.position() + 1);
                    if (LF == b1 || CR == b1 && LF == b2) {
                        return new String( state.buf, 0, i );
                    } else {
                        buffer.get();
                        // XXX make this configurable
                        // microsoft IIS thinks its ok to put CTLs in headers
                    }
                }
            }
        }

        return new String( state.buf, 0, l );
    }

    /**
     * LWS            = [CRLF] 1*( SP | HT )
     * @param buffer
     * @return boolean
     */
    private boolean eatLws(ByteBuffer buffer)
    {
        int s = buffer.position();

        byte b1 = buffer.get();
        if (CR == b1 && buffer.hasRemaining()) {
            if (LF != buffer.get()) {
                buffer.position(buffer.position() - 2);
                return false;
            }
        } else if (LF != b1) {
            buffer.position(buffer.position() - 1);
        }

        boolean result = false;
        while (buffer.hasRemaining()) {
            byte c = buffer.get();
            if (SP != c && HT != c) {
                buffer.position(buffer.position() - 1);
                break;
            } else {
                result = true;
            }
        }

        if (!result) {
            buffer.position(s);
        }

        return result;
    }

    /**
     * CRLF           = CR LF
     * in our implementation, CR is optional
     * @param buffer
     */
    private void eatCrLf( ByteBuffer buffer )
    {
        byte b1 = buffer.get();
        boolean ate = LF == b1 || CR == b1 && LF == buffer.get();
        if (!ate) {
            throw new RuntimeException("CRLF expected: " + b1);
        }
    }

    /**
     * DIGIT          = <any US-ASCII digit "0".."9">
     * this method reads 1*DIGIT
     * @param buffer
     * @return int
     */
    private int eatDigits( ByteBuffer buffer )
    {
        boolean foundOne = false;
        int i = 0;

        while (buffer.hasRemaining()) {
            if (isDigit(buffer.get(buffer.position()))) {
                foundOne = true;
                i = i * 10 + (buffer.get() - '0');
            } else {
                break;
            }
        }

        if (!foundOne) {
            throw new RuntimeException("no digits found");
        }

        return i;
    }

    /**
     * token          = 1*<any CHAR except CTLs or separators>
     * @param session
     * @param buffer
     * @return the string
     */
    private String token( AppTCPSession session, ByteBuffer buffer )
    {
        HttpParserSessionState state = (HttpParserSessionState) session.attachment( STATE_KEY );

        int l = buffer.remaining();

        for (int i = 0; buffer.hasRemaining(); i++) {
            state.buf[i] = buffer.get();
            if ( isCtl( state.buf[i] ) || isSeparator( state.buf[i] ) ) {
                buffer.position(buffer.position() - 1);
                return new String( state.buf, 0, i );
            }
        }

        return new String( state.buf, 0, l );
    }

    /**
     * True if byte is a seperator char
     * separators     = "(" | ")" | "<" | ">" | "@"
     *                | "," | ";" | ":" | "\" | <">
     *                | "/" | "[" | "]" | "?" | "="
     *                | "{" | "}" | SP | HT
     * @param b - the byte
     * @return true if seperator, false otherwise
     */
    private boolean isSeparator(byte b)
    {
        switch (b) {
        case '(': case ')': case '<': case '>': case '@':
        case ',': case ';': case ':': case '\\': case '"':
        case '/': case '[': case ']': case '?': case '=':
        case '{': case '}': case SP: case HT:
            return true;
        default:
            return false;
        }
    }

    /**
     * True if byte is digit, false otherwise
     * DIGIT          = <any US-ASCII digit "0".."9">
     * @param b - the byte
     * @return true if digit, false otherwise
     */
    private boolean isDigit(byte b)
    {
        return '0' <= b && '9' >= b;
    }

    /**
     * True if byte is hex char (1-9a-fA-F)
     * @param b the byte
     * @return true if hex, false otherwise
     */
    private boolean isHex(byte b)
    {
        switch (b) {
        case '0': case '1': case '2': case '3': case '4': case '5':
        case '6': case '7': case '8': case '9': case 'a': case 'b':
        case 'c': case 'd': case 'e': case 'f': case 'A': case 'B':
        case 'C': case 'D': case 'E': case 'F':
            return true;
        default:
            return false;
        }
    }

    /**
     * decimal value for hex char (1-f)
     * @param c - the char
     * @return int decimal value
     */
    private int hexValue(char c)
    {
        switch (c) {
        case '0': return 0;
        case '1': return 1;
        case '2': return 2;
        case '3': return 3;
        case '4': return 4;
        case '5': return 5;
        case '6': return 6;
        case '7': return 7;
        case '8': return 8;
        case '9': return 9;
        case 'A': case 'a': return 10;
        case 'B': case 'b': return 11;
        case 'C': case 'c': return 12;
        case 'D': case 'd': return 13;
        case 'E': case 'e': return 14;
        case 'F': case 'f': return 15;
        default: throw new IllegalArgumentException("expects hex digit");
        }
    }

    /**
     * True if byte is a control char
     *  CTL            = <any US-ASCII control character
     *                  (octets 0 - 31) and DEL (127)>
     * @param b - the byte
     * @return true if control char, false otherwise
     */
    boolean isCtl(byte b)
    {
        return 0 <= b && 31 >= b || 127 == b;
    }

    /**
     * true if byte is uppercase alpha (a-z).
     * @param b - the byte
     * @return true if uppercase alpha, false otherwise
     */
    boolean isUpAlpha(byte b)
    {
        return 'A' <= b && 'Z' >= b;
    }

    /**
     * true if byte is lowercase alpha (a-z).
     * @param b - the byte
     * @return true if lowercase alpha, false otherwise
     */
    boolean isLoAlpha(byte b)
    {
        return 'a' <= b && 'z' >= b;
    }

    /**
     * true if byte is alpha (a-zA-Z).
     * @param b - the byte
     * @return true if alphanumeric, false otherwise
     */
    protected boolean isAlpha(byte b)
    {
        return isUpAlpha(b) || isLoAlpha(b);
    }

    /**
     * set lineBuffering on the session
     * @param session 
     * @param oneLine true if line buffer is on
     */
    protected void lineBuffering( AppTCPSession session, boolean oneLine )
    {
        if (clientSide) {
            session.clientLineBuffering(oneLine);
        } else {
            session.serverLineBuffering(oneLine);
        }
    }

    /**
     * Get the read limit
     * @param session
     * @return the limit
     */
    protected long readLimit( AppTCPSession session )
    {
        if (clientSide) {
            return session.clientReadLimit();
        } else {
            return session.serverReadLimit();
        }
    }

    /**
     * Set the read limit
     * @param session
     * @param limit
     */
    protected void readLimit( AppTCPSession session, long limit )
    {
        if (clientSide) {
            session.clientReadLimit(limit);
        } else {
            session.serverReadLimit(limit);
        }
    }
    
    /**
     * Schedule a session timer
     * @param session
     * @param delay
     */
    protected void scheduleTimer( AppTCPSession session, long delay )
    {
        session.scheduleTimer(delay);
    }

    /**
     * Cancel the session timer
     * @param session
     */
    protected void cancelTimer( AppTCPSession session )
    {
        session.cancelTimer();
    }

    /**
     * Create an end marker streamer
     * @return the TokenStreamer
     */
    private TokenStreamer endMarkerStreamer()
    {
        return new TokenStreamer() {
            private boolean sent = false;

            /**
             * True if closeWhenDone
             * @return true
             */
            public boolean closeWhenDone() { return true; }

            /**
             * Get next token
             * @return token
             */
            public Token nextToken()
            {
                if (sent) {
                    return null;
                } else {
                    sent = true;
                    return EndMarkerToken.MARKER;
                }
            }
        };
    }

    /**
     * This method extracts the filename from the content disposition header
     * @param header - the HTTP response header token
     * @return the filename as a string if found, or null
     */
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

    /**
     * Dequeue the request
     * @param session
     * @param statusCode
     * @return the removed request
     */
    @SuppressWarnings("unchecked")
    RequestLineToken dequeueRequest( AppTCPSession session, int statusCode )
    {
        List<RequestLineToken> requests = (List<RequestLineToken>) session.globalAttachment( "http-request-queue" );

        if ( requests != null && requests.size() > 0 ) {
            return requests.remove(0);
        } else {
            if ( statusCode < 400 || statusCode > 499 ) {
                logger.warn("requests is empty: " + statusCode);
            }
            return null;
        }
    }
    
}
