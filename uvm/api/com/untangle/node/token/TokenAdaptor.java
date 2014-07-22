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
        Token token = (Token) obj;
        if (token instanceof ReleaseToken) {
            tokenHandler.releaseFlush( session );
            finalize( session );
            session.release();
        }

        tokenHandler.handleServerToken( session, token );
        return;
    }

    @Override
    public void handleTCPClientObject( NodeTCPSession session, Object obj )
    {
        Token token = (Token) obj;
        if (token instanceof ReleaseToken) {
            tokenHandler.releaseFlush( session );
            finalize( session );
            session.release();
        }

        tokenHandler.handleClientToken( session, token );
        return;
    }
    
    
    @Override
    public void handleTCPClientFIN( NodeTCPSession session )
    {
        tokenHandler.handleClientFin( session );
    }

    @Override
    public void handleTCPServerFIN( NodeTCPSession session )
    {
        tokenHandler.handleServerFin( session );
    }

    @Override
    public void handleTCPFinalized( NodeTCPSession session ) 
    {
        finalize( session );
        
        super.handleTCPFinalized( session );
    }

    private void finalize( NodeTCPSession sess )
    {
        tokenHandler.handleFinalized( sess );
    }

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
        try {
            tokenHandler.handleTimer( sess );
        } catch ( Exception exn ) {
            logger.warn("exception in timer, no action taken", exn);
        }
    }
}

