/**
 * $Id: EventHandler.java 38161 2014-07-23 23:01:44Z dmorris $
 */
package com.untangle.app.intrusion_prevention;

import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AppUDPSession;
import com.untangle.uvm.vnet.IPPacketHeader;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.UDPNewSessionRequest;

public class EventHandler extends AbstractEventHandler
{

    public EventHandler( IntrusionPreventionApp app )
    {
        super(app);
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
    }

    public void handleTCPNewSession( AppTCPSession session )
    {
        handleNewSession( session, Protocol.TCP );
    }

    public void handleUDPNewSession( AppUDPSession session )
    {
        handleNewSession( session, Protocol.UDP);
    }

    private void handleNewSession(AppSession session, Protocol protocol)
    {
    }

    public void handleTCPFinalized( AppTCPSession session )
    {
        handleFinalized( session, Protocol.TCP );
    }

    public void handleUDPFinalized( AppUDPSession session )
    {
        handleFinalized( session, Protocol.UDP );
    }

    private void handleFinalized( AppSession session, Protocol protocol )
    {
    }

    public void handleTCPClientChunk( AppTCPSession session, ByteBuffer data  )
    {
        super.handleTCPClientChunk( session, data );
    }

    public void handleTCPServerChunk( AppTCPSession session, ByteBuffer data  )
    {
        super.handleTCPServerChunk( session, data );
    }

    public void handleUDPClientPacket( AppUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        super.handleUDPClientPacket( session, data, header );
    }

    public void handleUDPServerPacket( AppUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        super.handleUDPServerPacket( session, data, header );
    }

}
