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

import com.untangle.node.token.AbstractUnparser;
import com.untangle.node.token.ChunkToken;
import com.untangle.node.token.EndMarkerToken;
import com.untangle.node.token.HeaderToken;
import com.untangle.node.token.Token;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.TCPStreamer;

/**
 * An HTTP <code>Unparser</code>.
 */
class HttpUnparser extends AbstractUnparser
{
    private static final Logger logger = Logger.getLogger(HttpUnparser.class);

    private static final ByteBuffer[] BYTE_BUFFER_PROTO = new ByteBuffer[0];
    private static final String STATE_KEY = "HTTP-unparser-state";
    
    private static final byte[] LAST_CHUNK = "0\r\n\r\n".getBytes();
    private static final byte[] CRLF = "\r\n".getBytes();

    private static final int CLOSE_ENCODING = 0;
    private static final int CONTENT_LENGTH_ENCODING = 1;
    private static final int CHUNKED_ENCODING = 2;

    private final HttpNodeImpl node;

    // used to keep request with header
    private class HttpUnparserSessionState
    {
        protected Queue<ByteBuffer> outputQueue = new LinkedList<ByteBuffer>();
        protected int size = 0;
        protected int transferEncoding;
    }

    public HttpUnparser( boolean clientSide, HttpNodeImpl node )
    {
        super( clientSide );
        this.node = node;
    }

    public void handleNewSession( NodeTCPSession session )
    {
        HttpUnparserSessionState state = new HttpUnparserSessionState();
        state.size = 0;
        state.outputQueue = new LinkedList<ByteBuffer>();
        session.attach( STATE_KEY, state );
    }

    public void unparse( NodeTCPSession session, Token token )
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

        if ( clientSide )
            session.sendDataToClient( BYTE_BUFFER_PROTO );
        else
            session.sendDataToServer( BYTE_BUFFER_PROTO );
    }

    private void requestLine( NodeTCPSession session, RequestLineToken rl )
    {
        HttpMethod method = rl.getMethod();

        if (logger.isDebugEnabled()) {
            logger.debug(" request-line" + " Unparser got method: " + method);
        }

        queueRequest( session, rl );

        queueOutput( session, rl.getBytes() );

        if ( clientSide )
            session.sendDataToClient( BYTE_BUFFER_PROTO );
        else
            session.sendDataToServer( BYTE_BUFFER_PROTO );
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
        if (isClientSide()) {
            dequeueOutput( session );
        } else {
            if ( clientSide )
                session.sendDataToClient( BYTE_BUFFER_PROTO );
            else
                session.sendDataToServer( BYTE_BUFFER_PROTO );
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
            if ( clientSide )
                session.sendDataToClient( BYTE_BUFFER_PROTO );
            else
                session.sendDataToServer( BYTE_BUFFER_PROTO );
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
            logger.debug(" GOT END MARER!!");
        }

        ByteBuffer buf = null;

        if ( state.transferEncoding == CHUNKED_ENCODING ) {
            buf = ByteBuffer.wrap(LAST_CHUNK);
        }

        if ( state.outputQueue.isEmpty() ) {
            if ( buf == null ) {
                if ( clientSide )
                    session.sendDataToClient( BYTE_BUFFER_PROTO );
                else
                    session.sendDataToServer( BYTE_BUFFER_PROTO );
            } else {
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

        if ( clientSide )
            session.sendDataToClient( buf );
        else
            session.sendDataToServer( buf );
        return;
    }

    @SuppressWarnings("unchecked")
    void queueRequest( NodeTCPSession session, RequestLineToken request )
    {
        List<RequestLineToken> requests = (List<RequestLineToken>) session.globalAttachment( "HTTP-request-queue" );

        if ( requests == null ) {
            requests = new LinkedList<RequestLineToken>();
            session.globalAttach( "HTTP-request-queue", requests );
        }

        requests.add(request);
    }
    
}
