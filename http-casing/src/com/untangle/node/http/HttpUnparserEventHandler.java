/**
 * $Id$
 */
package com.untangle.node.http;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.EndMarkerToken;
import com.untangle.uvm.vnet.ReleaseToken;
import com.untangle.node.http.HeaderToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.TCPStreamer;

/**
 * An HTTP <code>Unparser</code>.
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

    private final HttpImpl node;
    private final boolean clientSide;
    
    // used to keep request with header
    private class HttpUnparserSessionState
    {
        protected Queue<ByteBuffer> outputQueue = new LinkedList<ByteBuffer>();
        protected int size = 0;
        protected int transferEncoding;
    }

    public HttpUnparserEventHandler( boolean clientSide, HttpImpl node )
    {
        this.clientSide = clientSide;
        this.node = node;
    }

    @Override
    public void handleTCPNewSession( NodeTCPSession session )
    {
        HttpUnparserSessionState state = new HttpUnparserSessionState();
        state.size = 0;
        state.outputQueue = new LinkedList<ByteBuffer>();
        session.attach( STATE_KEY, state );
    }

    @Override
    public void handleTCPClientChunk( NodeTCPSession session, ByteBuffer data )
    {
        logger.warn("Received data when expect object");
        throw new RuntimeException("Received data when expect object");
    }

    @Override
    public void handleTCPServerChunk( NodeTCPSession session, ByteBuffer data )
    {
        logger.warn("Received data when expect object");
        throw new RuntimeException("Received data when expect object");
    }

    @Override
    public void handleTCPClientObject( NodeTCPSession session, Object obj )
    {
        if (clientSide) {
            logger.warn("Received object but expected data.");
            throw new RuntimeException("Received object but expected data.");
        } else {
            unparse( session, obj, false );
            return;
        }
    }
    
    @Override
    public void handleTCPServerObject( NodeTCPSession session, Object obj )
    {
        if (clientSide) {
            unparse( session, obj, true );
            return;
        } else {
            logger.warn("Received object but expected data.");
            throw new RuntimeException("Received object but expected data.");
        }
    }

    @Override
    public void handleTCPClientDataEnd( NodeTCPSession session, ByteBuffer data )
    {
        if ( data.hasRemaining() ) {
            logger.warn("Received data when expect object");
            throw new RuntimeException("Received data when expect object");
        }
    }

    @Override
    public void handleTCPServerDataEnd( NodeTCPSession session, ByteBuffer data )
    {
        if ( data.hasRemaining() ) {
            logger.warn("Received data when expect object");
            throw new RuntimeException("Received data when expect object");
        }
    }
    
    @Override
    public void handleTCPClientFIN( NodeTCPSession session )
    {
        if (clientSide) {
            logger.warn("Received unexpected event.");
            throw new RuntimeException("Received unexpected event.");
        } else {
            session.shutdownServer();
        }
    }

    @Override
    public void handleTCPServerFIN( NodeTCPSession session )
    {
        if (clientSide) {
            session.shutdownClient();
        } else {
            logger.warn("Received unexpected event.");
            throw new RuntimeException("Received unexpected event.");
        }
    }

    // private methods --------------------------------------------------------

    private void unparse( NodeTCPSession session, Object obj, boolean s2c )
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

    private void unparse( NodeTCPSession session, Token token )
    {
        HttpUnparserSessionState state = (HttpUnparserSessionState) session.attachment( STATE_KEY );
        
        if (logger.isDebugEnabled()) {
            logger.debug(" got unparse event node: " + token);
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

    public void releaseFlush( NodeTCPSession session )
    {
        dequeueOutput( session );
    }

    private void statusLine( NodeTCPSession session, StatusLine statusLine )
    {
        if (logger.isDebugEnabled()) {
            logger.debug(" status-line");
        }

        queueOutput( session, statusLine.getBytes() );
    }

    private void requestLine( NodeTCPSession session, RequestLineToken rl )
    {
        HttpMethod method = rl.getMethod();

        if (logger.isDebugEnabled()) {
            logger.debug(" request-line" + " Unparser got method: " + method);
        }

        queueRequest( session, rl );

        queueOutput( session, rl.getBytes() );
    }

    private void header( NodeTCPSession session, HeaderToken header )
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

    private void chunk( NodeTCPSession session, ChunkToken c )
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

    private void endMarker( NodeTCPSession session )
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

    private void queueOutput( NodeTCPSession session, ByteBuffer buf )
    {
        HttpUnparserSessionState state = (HttpUnparserSessionState) session.attachment( STATE_KEY );
        state.size += buf.remaining();
        state.outputQueue.add(buf);
    }

    private void dequeueOutput( NodeTCPSession session )
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

    @SuppressWarnings("unchecked")
    void queueRequest( NodeTCPSession session, RequestLineToken request )
    {
        List<RequestLineToken> requests = (List<RequestLineToken>) session.globalAttachment( "http-request-queue" );

        if ( requests == null ) {
            requests = new LinkedList<RequestLineToken>();
            session.globalAttach( "http-request-queue", requests );
        }

        requests.add(request);
    }
    
}
