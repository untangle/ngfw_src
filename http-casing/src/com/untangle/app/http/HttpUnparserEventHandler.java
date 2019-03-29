/**
 * $Id$
 */
package com.untangle.app.http;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.EndMarkerToken;
import com.untangle.uvm.vnet.ReleaseToken;
import com.untangle.app.http.HeaderToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.AppTCPSession;

/**
 * An HTTP <code>Unparser</code>.
 *
 * The HTTP unparser takes HTTP tokens and unparses them to byte buffers to be sent as data
 */
public class HttpUnparserEventHandler extends AbstractEventHandler
{
    private static final Logger logger = Logger.getLogger(HttpUnparserEventHandler.class);

    private static final String STATE_KEY = "http-unparser-state";
    
    private static final byte[] LAST_CHUNK = "0\r\n\r\n".getBytes();
    private static final byte[] CRLF = "\r\n".getBytes();

    private static final int CLOSE_ENCODING = 0;
    private static final int CONTENT_LENGTH_ENCODING = 1;
    private static final int CHUNKED_ENCODING = 2;

    private final HttpImpl app;
    private final boolean clientSide;
    
    /**
     * Stores the unparser state of the session
     */
    private class HttpUnparserSessionState
    {
        protected Queue<ByteBuffer> outputQueue = new LinkedList<>();
        protected int size = 0;
        protected int transferEncoding;
    }

    /**
     * Create an HttpUnparserEventHandler.
     * @param clientSide - true if this in an unparser for the client side
     * @param app - the http app
     */
    public HttpUnparserEventHandler( boolean clientSide, HttpImpl app )
    {
        this.clientSide = clientSide;
        this.app = app;
    }

    /**
     * handleTCPNewSession
     * attaches the HttpUnparserSessionState to the session
     * @param session
     */
    @Override
    public void handleTCPNewSession( AppTCPSession session )
    {
        HttpUnparserSessionState state = new HttpUnparserSessionState();
        state.size = 0;
        state.outputQueue = new LinkedList<>();
        session.attach( STATE_KEY, state );
    }

    /**
     * handleTCPClientChunk - should never be called
     * @param session
     * @param data
     */
    @Override
    public void handleTCPClientChunk( AppTCPSession session, ByteBuffer data )
    {
        logger.warn("Received data when expect object");
        throw new RuntimeException("Received data when expect object");
    }

    /**
     * handleTCPServerChunk - should never be called
     * @param session
     * @param data
     */
    @Override
    public void handleTCPServerChunk( AppTCPSession session, ByteBuffer data )
    {
        logger.warn("Received data when expect object");
        throw new RuntimeException("Received data when expect object");
    }

    /**
     * Handle a token from the client
     * Since this is the unparser - this should only ever be called for the server side unparser
     * @param session
     * @param obj
     */
    @Override
    public void handleTCPClientObject( AppTCPSession session, Object obj )
    {
        if (clientSide) {
            logger.warn("Received object but expected data.");
            throw new RuntimeException("Received object but expected data.");
        } else {
            unparse( session, obj );
            return;
        }
    }
    
    /**
     * Handle a token from the server
     * Since this is the unparser - this should only ever be called for the client side unparser
     * @param session
     * @param obj
     */
    @Override
    public void handleTCPServerObject( AppTCPSession session, Object obj )
    {
        if (clientSide) {
            unparse( session, obj );
            return;
        } else {
            logger.warn("Received object but expected data.");
            throw new RuntimeException("Received object but expected data.");
        }
    }

    /**
     * Handle handleTCPClientDataEnd.
     * @param session
     * @param data
     */
    @Override
    public void handleTCPClientDataEnd( AppTCPSession session, ByteBuffer data )
    {
        if ( data.hasRemaining() ) {
            logger.warn("Received data when expect object");
            throw new RuntimeException("Received data when expect object");
        }
    }

    /**
     * Handle handleTCPServerDataEnd.
     * @param session
     * @param data
     */
    @Override
    public void handleTCPServerDataEnd( AppTCPSession session, ByteBuffer data )
    {
        if ( data.hasRemaining() ) {
            logger.warn("Received data when expect object");
            throw new RuntimeException("Received data when expect object");
        }
    }
    
    /**
     * Handle handleTCPClientFIN.
     * @param session
     */
    @Override
    public void handleTCPClientFIN( AppTCPSession session )
    {
        if (clientSide) {
            logger.warn("Received unexpected event.");
            throw new RuntimeException("Received unexpected event.");
        } else {
            session.shutdownServer();
        }
    }

    /**
     * Handle handleTCPServerFIN.
     * @param session
     */
    @Override
    public void handleTCPServerFIN( AppTCPSession session )
    {
        if (clientSide) {
            session.shutdownClient();
        } else {
            logger.warn("Received unexpected event.");
            throw new RuntimeException("Received unexpected event.");
        }
    }

    /**
     * Handle the actual unparsing process
     * This will take the token (obj) and send the byte equivalent down the pipeline
     * @param session
     * @param obj - the token
     */
    private void unparse( AppTCPSession session, Object obj )
    {
        Token token = (Token) obj;

        try {
            if (token instanceof ReleaseToken) {
                ReleaseToken release = (ReleaseToken)token;

                session.release();
                releaseFlush( session );
                return;
            } else {
                unparse( session, token );
                return;
            }
        } catch (Exception exn) {
            logger.error("internal error, closing connection", exn);

            session.resetClient();
            session.resetServer();

            return;
        }
    }

    /**
     * Handle the actual unparsing process
     * This will take the token and send the byte equivalent down the pipeline
     * @param session
     * @param token - the token
     */
    private void unparse( AppTCPSession session, Token token )
    {
        HttpUnparserSessionState state = (HttpUnparserSessionState) session.attachment( STATE_KEY );
        
        if (logger.isDebugEnabled()) {
            logger.debug(" got unparse event app: " + token);
        }

        if (token instanceof StatusLine) {
            if (logger.isDebugEnabled()) {
                logger.debug(" got status line!");
            }
            state.transferEncoding = CLOSE_ENCODING;
            statusLine( session, (StatusLine) token );
            return;
        } else if (token instanceof RequestLineToken) {
            if (logger.isDebugEnabled()) {
                logger.debug(" got request line!");
            }
            requestLine( session, (RequestLineToken) token);
            return;
        } else if (token instanceof HeaderToken) {
            if (logger.isDebugEnabled()) {
                logger.debug(" got header!");
            }
            header( session, (HeaderToken) token );
            return;
        } else if (token instanceof ChunkToken) {
            if (logger.isDebugEnabled()) {
                logger.debug(" got chunk!");
            }
            chunk( session, (ChunkToken) token );
            return;
        } else if (token instanceof EndMarkerToken) {
            if (logger.isDebugEnabled()) {
                logger.debug(" got endmarker");
            }
            endMarker( session );
            return;
        } else {
            throw new IllegalArgumentException("unexpected: " + token);
        }
    }

    /**
     * dequeue all output for the session
     * @param session
     */
    public void releaseFlush( AppTCPSession session )
    {
        dequeueOutput( session );
    }

    /**
     * queue the status line data in the session
     * @param session
     * @param statusLine
     */
    private void statusLine( AppTCPSession session, StatusLine statusLine )
    {
        if (logger.isDebugEnabled()) {
            logger.debug(" status-line");
        }

        queueOutput( session, statusLine.getBytes() );
    }

    /**
     * queue the request line data in the session
     * @param session
     * @param rl - requestLine
     */
    private void requestLine( AppTCPSession session, RequestLineToken rl )
    {
        HttpMethod method = rl.getMethod();

        if (logger.isDebugEnabled()) {
            logger.debug(" request-line" + " Unparser got method: " + method);
        }

        queueRequest( session, rl );

        queueOutput( session, rl.getBytes() );
    }

    /**
     * queue the header data in the session
     * @param session
     * @param header
     */
    private void header( AppTCPSession session, HeaderToken header )
    {
        HttpUnparserSessionState state = (HttpUnparserSessionState) session.attachment( STATE_KEY );

        if (logger.isDebugEnabled()) {
            logger.debug(" header");
        }

        String encoding = header.getValue("transfer-encoding");
        if ( encoding != null && encoding.equalsIgnoreCase("chunked") ) {
            state.transferEncoding = CHUNKED_ENCODING;
        } else if ( header.getValue("content-length") != null ) {
            state.transferEncoding = CONTENT_LENGTH_ENCODING;
        }

        queueOutput( session, header.getBytes() );
        if ( clientSide ) {
            dequeueOutput( session );
        } 
    }

    /**
     * queue the generic ChunkToken in the session
     * This is handled slightly differently depending on the encoding method
     * @param session
     * @param c
     */
    private void chunk( AppTCPSession session, ChunkToken c )
    {
        HttpUnparserSessionState state = (HttpUnparserSessionState) session.attachment( STATE_KEY );

        if (logger.isDebugEnabled()) {
            logger.debug(" chunk");
        }

        ByteBuffer cBuf = c.getBytes();

        if ( state.transferEncoding == CHUNKED_ENCODING && cBuf.remaining() == 0 ) {
            return;
        }

        ByteBuffer buf;

        switch ( state.transferEncoding ) {
        case CLOSE_ENCODING:
        case CONTENT_LENGTH_ENCODING:
            buf = cBuf;
            break;
        case CHUNKED_ENCODING:
            buf = ByteBuffer.allocate(cBuf.remaining() + 32);
            String hexLen = Integer.toHexString(cBuf.remaining());
            buf.put(hexLen.getBytes());
            buf.put(CRLF);
            buf.put(cBuf);
            buf.put(CRLF);
            buf.flip();
            break;
        default:
            throw new IllegalStateException("transferEncoding: " + state.transferEncoding);
        }

        if ( state.outputQueue.isEmpty() ) {
            if ( clientSide )
                session.sendDataToClient( buf );
            else
                session.sendDataToServer( buf );
        } else {
            queueOutput( session, buf );
            dequeueOutput( session );
        }
    }

    /**
     * queue the endMarkerToken in the session
     * @param session
     */
    private void endMarker( AppTCPSession session )
    {
        HttpUnparserSessionState state = (HttpUnparserSessionState) session.attachment( STATE_KEY );

        if (logger.isDebugEnabled()) {
            logger.debug(" GOT END MARKER!!");
        }

        ByteBuffer buf = null;

        if ( state.transferEncoding == CHUNKED_ENCODING ) {
            buf = ByteBuffer.wrap(LAST_CHUNK);
        }

        if ( state.outputQueue.isEmpty() ) {
            if ( buf != null ) {
                if ( clientSide )
                    session.sendDataToClient( buf );
                else
                    session.sendDataToServer( buf );
            }
        } else {
            if (null != buf) {
                queueOutput( session, buf );
            }

            dequeueOutput( session );
            return;
        }
    }

    /**
     * queue the provided bytebuffer in the session
     * @param session
     * @param buf
     */
    private void queueOutput( AppTCPSession session, ByteBuffer buf )
    {
        HttpUnparserSessionState state = (HttpUnparserSessionState) session.attachment( STATE_KEY );
        state.size += buf.remaining();
        state.outputQueue.add(buf);
    }

    /**
     * flush all data
     * @param session
     */
    private void dequeueOutput( AppTCPSession session )
    {
        HttpUnparserSessionState state = (HttpUnparserSessionState) session.attachment( STATE_KEY );

        ByteBuffer buf = ByteBuffer.allocate( state.size );

        for (Iterator<ByteBuffer> i = state.outputQueue.iterator(); i.hasNext(); ) {
            ByteBuffer b = i.next();
            buf.put(b);
        }

        buf.flip();

        state.size = 0;
        state.outputQueue.clear();

        if ( buf.remaining() > 0 ) {
            if ( clientSide )
                session.sendDataToClient( buf );
            else
                session.sendDataToServer( buf );
        }
        
        return;
    }

    /**
     * queue the request Line token
     * @param session
     * @param request
     */
    @SuppressWarnings("unchecked")
    void queueRequest( AppTCPSession session, RequestLineToken request )
    {
        List<RequestLineToken> requests = (List<RequestLineToken>) session.globalAttachment( "http-request-queue" );

        if ( requests == null ) {
            requests = new LinkedList<>();
            session.globalAttach( "http-request-queue", requests );
        }

        requests.add(request);
    }
}
