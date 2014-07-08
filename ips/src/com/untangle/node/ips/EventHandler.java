/**
 * $Id$
 */
package com.untangle.node.ips;

import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.NodeUDPSession;
import com.untangle.uvm.vnet.IPPacketHeader;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.UDPNewSessionRequest;

public class EventHandler extends AbstractEventHandler
{
    private IpsDetectionEngine ipsEngine;

    public EventHandler( IpsNodeImpl node )
    {
        super(node);
        ipsEngine = node.getEngine();
    }

    public void handleTCPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        handleNewSessionRequest( sessionRequest, Protocol.TCP );
    }

    public void handleUDPNewSessionRequest( UDPNewSessionRequest sessionRequest )
    {
        handleNewSessionRequest( sessionRequest, Protocol.UDP );
    }

    private void handleNewSessionRequest( IPNewSessionRequest request, Protocol protocol )
    {
        ipsEngine.processNewSessionRequest(request, protocol);
    }

    public void handleTCPNewSession( NodeTCPSession session )
    {
        handleNewSession( session, Protocol.TCP );
    }

    public void handleUDPNewSession( NodeUDPSession session )
    {
        handleNewSession( session, Protocol.UDP);
    }

    private void handleNewSession(NodeSession session, Protocol protocol)
    {
        ipsEngine.processNewSession(session, protocol);
    }

    public void handleTCPFinalized( NodeTCPSession session )
    {
        handleFinalized( session, Protocol.TCP );
    }

    public void handleUDPFinalized( NodeUDPSession session )
    {
        handleFinalized( session, Protocol.UDP );
    }

    private void handleFinalized( NodeSession session, Protocol protocol )
    {
        ipsEngine.processFinalized( session, protocol );
    }

    public void handleTCPClientChunk( NodeTCPSession session, ByteBuffer data  )
    {
        ipsEngine.handleChunk( data, session, false );
        super.handleTCPClientChunk( session, data );
    }

    public void handleTCPServerChunk( NodeTCPSession session, ByteBuffer data  )
    {
        ipsEngine.handleChunk( data, session, true );
        super.handleTCPServerChunk( session, data );
    }

    public void handleUDPClientPacket( NodeUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        ipsEngine.handleChunk( data, session, false );
        super.handleUDPClientPacket( session, data, header );
    }

    public void handleUDPServerPacket( NodeUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        ipsEngine.handleChunk( data, session, true );
        super.handleUDPServerPacket( session, data, header );
    }

}
