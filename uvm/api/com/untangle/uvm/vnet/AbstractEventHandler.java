/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.nio.ByteBuffer;

import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.app.App;
import com.untangle.uvm.vnet.SessionEventHandler;

/**
 * <code>AbstractEventHandler</code> is the abstract base class that provides
 * the default actions that any session event handler will need.
 *
 * By default the event handler methods will just pass the events on to the other side.
 * Subclasses will need to override these methods with the appropriate actions.
 */
public abstract class AbstractEventHandler implements SessionEventHandler
{
    protected AppBase app;

    protected AbstractEventHandler(App app)
    {
        this.app = (AppBase)app;
    }

    protected AbstractEventHandler()
    {
        this.app = null;
    }
    
    public void handleTimer( AppSession session )
    {
        // do nothing
    }

    public void handleTCPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        // do nothing
    }

    public void handleTCPNewSession( AppTCPSession session )
    {
        // do nothing
    }

    public void handleTCPClientDataEnd( AppTCPSession session, ByteBuffer data )
    {
        // do nothing
    }

    public void handleTCPServerDataEnd( AppTCPSession session, ByteBuffer data )
    {
        // do nothing
    }

    public void handleTCPClientFIN( AppTCPSession session )
    {
        // propagate shutdown to other side
        session.shutdownServer();
    }

    public void handleTCPServerFIN( AppTCPSession session )
    {
        // propagate shutdown to other side
        session.shutdownClient();
    }

    public void handleTCPClientRST( AppTCPSession session )
    {
        // propagate reset to other side
        session.resetServer();
    }

    public void handleTCPServerRST( AppTCPSession session )
    {
        // propagate reset to other side
        session.resetClient();
    }

    public void handleTCPFinalized( AppTCPSession session )
    {
        // do nothing
    }

    public void handleTCPComplete( AppTCPSession session )
    {
        // do nothing
    }

    public void handleTCPClientChunk( AppTCPSession session, ByteBuffer data )
    {
        session.sendDataToServer( data );
    }

    public void handleTCPServerChunk( AppTCPSession session, ByteBuffer data )
    {
        session.sendDataToClient( data );
    }

    public void handleTCPClientObject( AppTCPSession session, Object obj )
    {
        session.sendObjectToServer( obj );
    }

    public void handleTCPServerObject( AppTCPSession session, Object obj )
    {
        session.sendObjectToClient( obj );
    }
    
    public void handleTCPClientWritable( AppTCPSession session )
    {
        // do nothing
    }

    public void handleTCPServerWritable( AppTCPSession session )
    {
        // do nothing
    }
    
    public void handleUDPNewSessionRequest( UDPNewSessionRequest sessionRequest )
    {
        // do nothing
    }

    public void handleUDPNewSession( AppUDPSession session )
    {
        // do nothing
    }

    public void handleUDPClientExpired( AppUDPSession session )
    {
        // Current assumption: A single expire will be generated on
        // one side of the pipeline, which will travel across it.
        // Another possibility would be to hit them all at once.  Just
        // go ahead and expire the other side.  The app will override
        // this method if it wants to keep the other side open.
        session.expireServer();
    }

    public void handleUDPServerExpired( AppUDPSession session )
    {
        // Current assumption: A single expire will be generated on
        // one side of the pipeline, which will travel across it.
        // Another possibility would be to hit them all at once.  Just
        // go ahead and expire the other side.  The app will override
        // this method if it wants to keep the other side open.
        session.expireClient();
    }

    public void handleUDPClientWritable( AppUDPSession session )
    {
        // do nothing
    }

    public void handleUDPServerWritable( AppUDPSession session )
    {
        // do nothing
    }

    public void handleUDPFinalized( AppUDPSession session )
    {
        // do nothing
    }

    public void handleUDPComplete( AppUDPSession session )
    {
        // do nothing
    }

    public void handleUDPClientPacket( AppUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        session.sendServerPacket( data, header );
    }

    public void handleUDPServerPacket( AppUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        session.sendClientPacket( data, header );
    }

}
