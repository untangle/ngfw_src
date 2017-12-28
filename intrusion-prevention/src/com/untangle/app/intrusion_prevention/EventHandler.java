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

/**
 * The traffic event handler for Intrusion Prevention
 */
public class EventHandler extends AbstractEventHandler
{

    /**
     * Create a application control lite event handler
     * @param app 
     *      The Intrusion Prevention app
     */
    public EventHandler( IntrusionPreventionApp app )
    {
        super(app);
    }

    /**
     * Handle new TCP session request
     * @param sessionRequest
     *  The TCP session request
     */
    public void handleTCPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        handleNewSessionRequest( sessionRequest, Protocol.TCP );
    }

    /**
     * Handle new UDP session request
     * @param sessionRequest
     *  The UDP session request
     */
    public void handleUDPNewSessionRequest( UDPNewSessionRequest sessionRequest )
    {
        handleNewSessionRequest( sessionRequest, Protocol.UDP );
    }

    /**
     * Handle new session request
     * @param request
     *  The session request
     * @param protocol
     *  The protocol
     */
    private void handleNewSessionRequest( IPNewSessionRequest request, Protocol protocol )
    {
    }

    /**
     * Handle a new TCP session
     * This creates all the session info and attaches it to the session
     * @param session
     *  The TCP session
     */
    public void handleTCPNewSession( AppTCPSession session )
    {
        handleNewSession( session, Protocol.TCP );
    }

    /**
     * Handle a new UDP session
     * This creates all the session info and attaches it to the session
     * @param session
     *  The UDP session
     */
    public void handleUDPNewSession( AppUDPSession session )
    {
        handleNewSession( session, Protocol.UDP);
    }

    /**
     * Handle a new session
     * This creates all the session info and attaches it to the session
     * @param session
     *  The session
     * @param protocol
     *  The protocol
     */
    private void handleNewSession(AppSession session, Protocol protocol)
    {
    }

    /**
     * Handle finalized session chunk
     * @param session
     *  The TCP session
     */
    public void handleTCPFinalized( AppTCPSession session )
    {
        handleFinalized( session, Protocol.TCP );
    }

    /**
     * Handle finalized UDP chunk
     * @param session
     *  The UDP session
     */
    public void handleUDPFinalized( AppUDPSession session )
    {
        handleFinalized( session, Protocol.UDP );
    }

    /**
     * Handle finalized session chunk
     * @param session
     *  The session
     * @param protocol
     *  Protocol
     */
    private void handleFinalized( AppSession session, Protocol protocol )
    {
    }

    /**
     * Handle a chunk of TCP data
     * @param session
     *  The TCP session
     * @param data
     *  The TCP data
     */
    public void handleTCPClientChunk( AppTCPSession session, ByteBuffer data  )
    {
        super.handleTCPClientChunk( session, data );
    }

    /**
     * Handle a chunk of TCP data
     * @param session
     *  The TCP session
     * @param data
     *  The TCP data
     */
    public void handleTCPServerChunk( AppTCPSession session, ByteBuffer data  )
    {
        super.handleTCPServerChunk( session, data );
    }

    /**
     * Handle a chunk of UDP data
     * @param session
     *  The UDP session
     * @param data
     *  The UDP data
     * @param header
     *  The packet header
     */
    public void handleUDPClientPacket( AppUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        super.handleUDPClientPacket( session, data, header );
    }

    /**
     * Handle a chunk of UDP data
     * @param session
     *  The UDP session
     * @param data
     *  The UDP data
     * @param header
     *  The packet header
     */
    public void handleUDPServerPacket( AppUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        super.handleUDPServerPacket( session, data, header );
    }

}
