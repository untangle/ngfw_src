/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.NodeUDPSession;
import com.untangle.uvm.vnet.UDPNewSessionRequest;
import com.untangle.uvm.vnet.IPPacketHeader;
import com.untangle.uvm.vnet.TCPStreamer;

/**
 * Adapts a Token session's underlying byte-stream a <code>TokenHandler</code>.
 */
public class TokenAdaptor extends AbstractEventHandler
{
    private static final ByteBuffer[] BYTE_BUFFER_PROTO = new ByteBuffer[0];

    private final Logger logger = Logger.getLogger(TokenAdaptor.class);

    private TokenHandler tokenHandler;
    
    public TokenAdaptor( Node node, TokenHandler handler )
    {
        super(node);
        this.tokenHandler = handler;
    }

    @Override
    public void handleTCPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        tokenHandler.handleNewSessionRequest( sessionRequest );
    }

    @Override
    public void handleTCPNewSession( NodeTCPSession session )
    {
        tokenHandler.handleNewSession( session );
    }

    @Override
    public void handleTCPServerChunk( NodeTCPSession session, ByteBuffer data )
    {
        throw new UnsupportedOperationException("data not supported." + data.remaining());
    }
    
    @Override
    public void handleTCPClientChunk( NodeTCPSession session, ByteBuffer data )
    {
        throw new UnsupportedOperationException("data not supported." + data.remaining());
    }

    @Override
    public void handleTCPServerObject( NodeTCPSession session, Object obj )
    {
        handleToken( tokenHandler, session, obj, true );
        return;
    }

    @Override
    public void handleTCPClientObject( NodeTCPSession session, Object obj )
    {
        handleToken( tokenHandler, session, obj, false );
        return;
    }
    
    
    @Override
    public void handleTCPClientFIN( NodeTCPSession session )
    {
        try {
            tokenHandler.handleClientFin( session );
        } catch ( Exception exn ) {
            logger.warn("resetting connection", exn);
            session.resetClient();
            session.resetServer();
        }
    }

    @Override
    public void handleTCPServerFIN( NodeTCPSession session )
    {
        try {
            tokenHandler.handleServerFin( session );
        } catch ( Exception exn ) {
            logger.warn("resetting connection", exn);
            session.resetClient();
            session.resetServer();
        }
    }

    @Override
    public void handleTCPFinalized( NodeTCPSession session ) 
    {
        finalize( session );
        
        super.handleTCPFinalized( session );
    }

    private void finalize( NodeTCPSession sess )
    {
        try {
            tokenHandler.handleFinalized( sess );
        } catch ( Exception exn ) {
            logger.warn("Exception. resetting connection", exn);
            sess.resetClient();
            sess.resetServer();
        }
    }
    // UDP events -------------------------------------------------------------

    @Override
    public void handleUDPNewSessionRequest( UDPNewSessionRequest sessionRequest )
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPNewSession( NodeUDPSession session ) 
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPClientPacket( NodeUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPServerPacket(  NodeUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPClientExpired( NodeUDPSession session ) 
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPServerExpired( NodeUDPSession session ) 
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPFinalized( NodeUDPSession session ) 
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleTimer( NodeSession sess )
    {
        TokenHandler handler = (TokenHandler) sess.attachment();

        try {
            handler.handleTimer( sess );
        } catch ( Exception exn ) {
            logger.warn("exception in timer, no action taken", exn);
        }
    }

    // private methods --------------------------------------------------------

    private void handleToken(TokenHandler handler, NodeTCPSession session, Object obj, boolean s2c)
    {
        Token token = (Token) obj;
        
        try {
            doToken(session, s2c, handler, token);
        } catch ( Exception exn ) {
            logger.warn("resetting connection", exn);
            session.resetClient();
            session.resetServer();
            return;
        }

        // FIXME - must be handled elsewhere
        // FIXME - must be handled elsewhere
        // FIXME - must be handled elsewhere
        
        // if (tr.isStreamer()) {
        //     if (tr.s2cStreamer() != null) {
        //         logger.debug("beginning client stream");
        //         TokenStreamer tokSt = tr.s2cStreamer();
        //         TCPStreamer ts = new TokenStreamerAdaptor( tokSt, session );
        //         session.beginClientStream(ts);
        //     } else {
        //         logger.debug("beginning server stream");
        //         TokenStreamer tokSt = tr.c2sStreamer();
        //         TCPStreamer ts = new TokenStreamerAdaptor( tokSt, session );
        //         session.beginServerStream(ts);
        //     }
        //     // just means nothing extra to send before beginning stream.
        //     return;
        // } else {
        //     session.sendObjectsToClient( tr.s2cTokens() );
        //     session.sendObjectsToServer( tr.c2sTokens() );
        //     return;
        // }
    }

    public void doToken( NodeTCPSession session, boolean s2c, TokenHandler handler, Token token )
       
    {
        if (token instanceof ReleaseToken) {
            ReleaseToken release = (ReleaseToken)token;

            handler.releaseFlush( session );

            finalize( session );
            session.release();

            // FIXME - must be handled elsewhere
            // FIXME - must be handled elsewhere
            // FIXME - must be handled elsewhere
            // if (utr.isStreamer()) {
            //     if (s2c) {
            //         TokenStreamer cStm = utr.c2sStreamer();
            //         TokenStreamer sStm = new ReleaseTokenStreamer(utr.s2cStreamer(), release);

            //         return new TokenResult(sStm, cStm);
            //     } else {
            //         TokenStreamer cStm = new ReleaseTokenStreamer(utr.c2sStreamer(), release);
            //         TokenStreamer sStm = utr.s2cStreamer();

            //         return new TokenResult(sStm, cStm);
            //     }
            // } else {
            //     if (s2c) {
            //         Token[] cTok = utr.c2sTokens();

            //         Token[] sTokOrig = utr.s2cTokens();
            //         Token[] sTok = new Token[sTokOrig.length + 1];
            //         System.arraycopy(sTokOrig, 0, sTok, 0, sTokOrig.length);
            //         sTok[sTok.length - 1] = release;

            //         return new TokenResult(sTok, cTok);
            //     } else {
            //         Token[] cTokOrig = utr.c2sTokens();
            //         Token[] cTok = new Token[cTokOrig.length + 1];
            //         System.arraycopy(cTokOrig, 0, cTok, 0, cTokOrig.length);
            //         cTok[cTok.length - 1] = release;
            //         Token[] sTok = utr.s2cTokens();
            //         return new TokenResult(sTok, cTok);
            //     }
            // }
        } else {
            if (s2c) {
                handler.handleServerToken( session, token );
                return;
            } else {
                handler.handleClientToken( session, token );
                return;
            }
        }
    }
}

