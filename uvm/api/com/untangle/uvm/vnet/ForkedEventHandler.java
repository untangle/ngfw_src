/**
 * $Id: ForkedEventHandler.java,v 1.00 2014/07/28 11:32:52 dmorris Exp $
 */
package com.untangle.uvm.vnet;

import java.nio.ByteBuffer;

import com.untangle.uvm.app.App;
import com.untangle.uvm.vnet.SessionEventHandler;

/**
 * <code>ForkedEventHandler</code> is the an event handler that
 * "forks" all client events to a clientEventHandler and server events
 * to a serverEventHandler This is useful if you have 2 or more event
 * handlers that you want to share the same AppSession and exist in
 * the same place in the pipeline. Example: clientEventHandler and
 * serverEventHandler want to exist at the same place in the pipeline
 * and share the same AppSession (and associated attachments/state).
 * You can create a ForkedEventHandler that contains both
 * clientEventHandler and serverEventHandler.
 * All client events go to clientEventHandler All server events go to
 * serverEventHandler
 *
 * All session global events go to both Be careful that you handle
 * those events correctly with that in mind!
 */
public class ForkedEventHandler extends AbstractEventHandler
{
    private SessionEventHandler clientEventHandler;
    private SessionEventHandler serverEventHandler;
    
    /**
     * ForkedEventHandler constructor
     * @param clientEventHandler
     * @param serverEventHandler
     */
    public ForkedEventHandler( SessionEventHandler clientEventHandler, SessionEventHandler serverEventHandler )
    {
        this.clientEventHandler = clientEventHandler;
        this.serverEventHandler = serverEventHandler;
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleTimer()
     * @param session
     */
    @Override
    public void handleTimer( AppSession session )
    {
        clientEventHandler.handleTimer( session );
        serverEventHandler.handleTimer( session );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleTCPNewSessionRequest()
     * @param sessionRequest
     */
    @Override
    public void handleTCPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        clientEventHandler.handleTCPNewSessionRequest( sessionRequest );
        serverEventHandler.handleTCPNewSessionRequest( sessionRequest );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleTCPNewSession()
     * @param session
     */
    @Override
    public void handleTCPNewSession( AppTCPSession session )
    {
        clientEventHandler.handleTCPNewSession( session );
        serverEventHandler.handleTCPNewSession( session );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleTCPClientDataEnd()
     * @param session
     * @param data
     */
    @Override
    public void handleTCPClientDataEnd( AppTCPSession session, ByteBuffer data )
    {
        clientEventHandler.handleTCPClientDataEnd( session, data );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleTCPServerDataEnd()
     * @param session
     * @param data
     */
    @Override
    public void handleTCPServerDataEnd( AppTCPSession session, ByteBuffer data )
    {
        serverEventHandler.handleTCPServerDataEnd( session, data );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleTCPClientFIN()
     * @param session
     */
    @Override
    public void handleTCPClientFIN( AppTCPSession session )
    {
        clientEventHandler.handleTCPClientFIN( session );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleTCPServerFIN()
     * @param session
     */
    @Override
    public void handleTCPServerFIN( AppTCPSession session )
    {
        serverEventHandler.handleTCPServerFIN( session );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleTCPClientRST()
     * @param session
     */
    @Override
    public void handleTCPClientRST( AppTCPSession session )
    {
        clientEventHandler.handleTCPClientRST( session );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleTCPServerRST()
     * @param session
     */
    @Override
    public void handleTCPServerRST( AppTCPSession session )
    {
        serverEventHandler.handleTCPServerRST( session );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleTCPFinalized()
     * @param session
     */
    @Override
    public void handleTCPFinalized( AppTCPSession session )
    {
        clientEventHandler.handleTCPFinalized( session );
        serverEventHandler.handleTCPFinalized( session );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleTCPComplete()
     * @param session
     */
    @Override
    public void handleTCPComplete( AppTCPSession session )
    {
        clientEventHandler.handleTCPComplete( session );
        serverEventHandler.handleTCPComplete( session );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleTCPClientChunk()
     * @param session
     * @param data
     */
    @Override
    public void handleTCPClientChunk( AppTCPSession session, ByteBuffer data )
    {
        clientEventHandler.handleTCPClientChunk( session, data );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleTCPServerChunk()
     * @param session
     * @param data
     */
    @Override
    public void handleTCPServerChunk( AppTCPSession session, ByteBuffer data )
    {
        serverEventHandler.handleTCPServerChunk( session, data );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleTCPClientObject()
     * @param session
     * @param obj
     */
    @Override
    public void handleTCPClientObject( AppTCPSession session, Object obj )
    {
        clientEventHandler.handleTCPClientObject( session, obj );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleTCPServerObject()
     * @param session
     * @param obj
     */
    @Override
    public void handleTCPServerObject( AppTCPSession session, Object obj )
    {
        serverEventHandler.handleTCPServerObject( session, obj );
    }
    
    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleTCPClientWritable()
     * @param session
     */
    @Override
    public void handleTCPClientWritable( AppTCPSession session )
    {
        clientEventHandler.handleTCPClientWritable( session );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleTCPServerWritable()
     * @param session
     */
    @Override
    public void handleTCPServerWritable( AppTCPSession session )
    {
        serverEventHandler.handleTCPServerWritable( session );
    }
    
    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleUDPNewSessionRequest()
     * @param sessionRequest
     */
    @Override
    public void handleUDPNewSessionRequest( UDPNewSessionRequest sessionRequest )
    {
        clientEventHandler.handleUDPNewSessionRequest( sessionRequest );
        serverEventHandler.handleUDPNewSessionRequest( sessionRequest );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleUDPNewSession()
     * @param session
     */
    @Override
    public void handleUDPNewSession( AppUDPSession session )
    {
        clientEventHandler.handleUDPNewSession( session );
        serverEventHandler.handleUDPNewSession( session );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleUDPClientExpired()
     * @param session
     */
    @Override
    public void handleUDPClientExpired( AppUDPSession session )
    {
        clientEventHandler.handleUDPClientExpired( session );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleUDPServerExpired()
     * @param session
     */
    @Override
    public void handleUDPServerExpired( AppUDPSession session )
    {
        serverEventHandler.handleUDPServerExpired( session );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleUDPClientWritable()
     * @param session
     */
    @Override
    public void handleUDPClientWritable( AppUDPSession session )
    {
        clientEventHandler.handleUDPClientWritable( session );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleUDPServerWritable()
     * @param session
     */
    @Override
    public void handleUDPServerWritable( AppUDPSession session )
    {
        serverEventHandler.handleUDPServerWritable( session );
    }
    
    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleUDPFinalized()
     * @param session
     */
    @Override
    public void handleUDPFinalized( AppUDPSession session )
    {
        clientEventHandler.handleUDPFinalized( session );
        serverEventHandler.handleUDPFinalized( session );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleUDPComplete()
     * @param session
     */
    @Override
    public void handleUDPComplete( AppUDPSession session )
    {
        clientEventHandler.handleUDPComplete( session );
        serverEventHandler.handleUDPComplete( session );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleUDPClientPacket()
     * @param session
     * @param data
     * @param header
     */
    @Override
    public void handleUDPClientPacket( AppUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        clientEventHandler.handleUDPClientPacket( session, data, header );
    }

    /**
     * @see com.unatngle.uvm.vnet.AbstractEventHandler.handleUDPServerPacket()
     * @param session
     * @param data
     * @param header
     */
    @Override
    public void handleUDPServerPacket( AppUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        serverEventHandler.handleUDPServerPacket( session, data, header );
    }
}

    
