/**
 * $Id: ForkedEventHandler.java,v 1.00 2014/07/28 11:32:52 dmorris Exp $
 */
package com.untangle.uvm.vnet;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.vnet.SessionEventHandler;

/**
 * <code>ForkedEventHandler</code> is the an event handler that "forks"
 * all client events to a clientEventHandler and server events to a serverEventHandler
 *
 * This is useful if you have 2 or more event handlers that you want to share
 * the same NodeSession and exist in the same place in the pipeline.
 *
 * Example: clientEventHandler and serverEventHandler want to exist at the same place in the pipeline
 * and share the same NodeSession (and associated attachments/state).
 * You can create a ForkedEventHandler that contains both clientEventHandler and serverEventHandler.
 * 
 * All client events go to clientEventHandler
 * All server events go to serverEventHandler
 * 
 * All session global events go to both
 * Be careful that you handle those events correctly with that in mind!
 */
public class ForkedEventHandler extends AbstractEventHandler
{
    private SessionEventHandler clientEventHandler;
    private SessionEventHandler serverEventHandler;
    
    public ForkedEventHandler( SessionEventHandler clientEventHandler, SessionEventHandler serverEventHandler )
    {
        this.clientEventHandler = clientEventHandler;
        this.serverEventHandler = serverEventHandler;
    }

    @Override
    public void handleTimer( NodeSession session )
    {
        clientEventHandler.handleTimer( session );
        serverEventHandler.handleTimer( session );
    }

    @Override
    public void handleTCPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        clientEventHandler.handleTCPNewSessionRequest( sessionRequest );
        serverEventHandler.handleTCPNewSessionRequest( sessionRequest );
    }

    @Override
    public void handleTCPNewSession( NodeTCPSession session )
    {
        clientEventHandler.handleTCPNewSession( session );
        serverEventHandler.handleTCPNewSession( session );
    }

    @Override
    public void handleTCPClientDataEnd( NodeTCPSession session, ByteBuffer data )
    {
        clientEventHandler.handleTCPClientDataEnd( session, data );
    }

    @Override
    public void handleTCPServerDataEnd( NodeTCPSession session, ByteBuffer data )
    {
        serverEventHandler.handleTCPServerDataEnd( session, data );
    }

    @Override
    public void handleTCPClientFIN( NodeTCPSession session )
    {
        clientEventHandler.handleTCPClientFIN( session );
    }

    @Override
    public void handleTCPServerFIN( NodeTCPSession session )
    {
        serverEventHandler.handleTCPServerFIN( session );
    }

    @Override
    public void handleTCPClientRST( NodeTCPSession session )
    {
        clientEventHandler.handleTCPClientRST( session );
    }

    @Override
    public void handleTCPServerRST( NodeTCPSession session )
    {
        serverEventHandler.handleTCPServerRST( session );
    }

    @Override
    public void handleTCPFinalized( NodeTCPSession session )
    {
        clientEventHandler.handleTCPFinalized( session );
        serverEventHandler.handleTCPFinalized( session );
    }

    @Override
    public void handleTCPComplete( NodeTCPSession session )
    {
        clientEventHandler.handleTCPComplete( session );
        serverEventHandler.handleTCPComplete( session );
    }

    @Override
    public void handleTCPClientChunk( NodeTCPSession session, ByteBuffer data )
    {
        clientEventHandler.handleTCPClientChunk( session, data );
    }

    @Override
    public void handleTCPServerChunk( NodeTCPSession session, ByteBuffer data )
    {
        serverEventHandler.handleTCPServerChunk( session, data );
    }

    @Override
    public void handleTCPClientObject( NodeTCPSession session, Object obj )
    {
        clientEventHandler.handleTCPClientObject( session, obj );
    }

    @Override
    public void handleTCPServerObject( NodeTCPSession session, Object obj )
    {
        serverEventHandler.handleTCPServerObject( session, obj );
    }
    
    @Override
    public void handleTCPClientWritable( NodeTCPSession session )
    {
        clientEventHandler.handleTCPClientWritable( session );
    }

    @Override
    public void handleTCPServerWritable( NodeTCPSession session )
    {
        serverEventHandler.handleTCPServerWritable( session );
    }
    
    @Override
    public void handleUDPNewSessionRequest( UDPNewSessionRequest sessionRequest )
    {
        clientEventHandler.handleUDPNewSessionRequest( sessionRequest );
        serverEventHandler.handleUDPNewSessionRequest( sessionRequest );
    }

    @Override
    public void handleUDPNewSession( NodeUDPSession session )
    {
        clientEventHandler.handleUDPNewSession( session );
        serverEventHandler.handleUDPNewSession( session );
    }

    @Override
    public void handleUDPClientExpired( NodeUDPSession session )
    {
        clientEventHandler.handleUDPClientExpired( session );
    }

    @Override
    public void handleUDPServerExpired( NodeUDPSession session )
    {
        serverEventHandler.handleUDPServerExpired( session );
    }

    @Override
    public void handleUDPClientWritable( NodeUDPSession session )
    {
        clientEventHandler.handleUDPClientWritable( session );
    }

    @Override
    public void handleUDPServerWritable( NodeUDPSession session )
    {
        serverEventHandler.handleUDPServerWritable( session );
    }
    
    @Override
    public void handleUDPFinalized( NodeUDPSession session )
    {
        clientEventHandler.handleUDPFinalized( session );
        serverEventHandler.handleUDPFinalized( session );
    }

    @Override
    public void handleUDPComplete( NodeUDPSession session )
    {
        clientEventHandler.handleUDPComplete( session );
        serverEventHandler.handleUDPComplete( session );
    }

    @Override
    public void handleUDPClientPacket( NodeUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        clientEventHandler.handleUDPClientPacket( session, data, header );
    }

    @Override
    public void handleUDPServerPacket( NodeUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        serverEventHandler.handleUDPServerPacket( session, data, header );
    }
}

    