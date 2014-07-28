/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.nio.ByteBuffer;

import com.untangle.uvm.node.Node;
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
    protected NodeBase node;

    protected AbstractEventHandler(Node node)
    {
        this.node = (NodeBase)node;
    }

    protected AbstractEventHandler()
    {
        this.node = null;
    }
    
    public void handleTimer( NodeSession session )
    {
        // do nothing
    }

    public void handleTCPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        // do nothing
    }

    public void handleTCPNewSession( NodeTCPSession session )
    {
        // do nothing
    }

    public void handleTCPClientDataEnd( NodeTCPSession session, ByteBuffer data )
    {
        // do nothing
    }

    public void handleTCPServerDataEnd( NodeTCPSession session, ByteBuffer data )
    {
        // do nothing
    }

    public void handleTCPClientFIN( NodeTCPSession session )
    {
        // propagate shutdown to other side
        session.shutdownServer();
    }

    public void handleTCPServerFIN( NodeTCPSession session )
    {
        // propagate shutdown to other side
        session.shutdownClient();
    }

    public void handleTCPClientRST( NodeTCPSession session )
    {
        // propagate reset to other side
        session.resetServer();
    }

    public void handleTCPServerRST( NodeTCPSession session )
    {
        // propagate reset to other side
        session.resetClient();
    }

    public void handleTCPFinalized( NodeTCPSession session )
    {
        // do nothing
    }

    public void handleTCPComplete( NodeTCPSession session )
    {
        // do nothing
    }

    public void handleTCPClientChunk( NodeTCPSession session, ByteBuffer data )
    {
        session.sendDataToServer( data );
    }

    public void handleTCPServerChunk( NodeTCPSession session, ByteBuffer data )
    {
        session.sendDataToClient( data );
    }

    public void handleTCPClientObject( NodeTCPSession session, Object obj )
    {
        session.sendObjectToServer( obj );
    }

    public void handleTCPServerObject( NodeTCPSession session, Object obj )
    {
        session.sendObjectToClient( obj );
    }
    
    public void handleTCPServerWritable( NodeTCPSession session )
    {
        // do nothing
    }

    public void handleTCPClientWritable( NodeTCPSession session )
    {
        // do nothing
    }

    public void handleUDPNewSessionRequest( UDPNewSessionRequest sessionRequest )
    {
        // do nothing
    }

    public void handleUDPNewSession( NodeUDPSession session )
    {
        // do nothing
    }

    public void handleUDPClientExpired( NodeUDPSession session )
    {
        // Current assumption: A single expire will be generated on
        // one side of the pipeline, which will travel across it.
        // Another possibility would be to hit them all at once.  Just
        // go ahead and expire the other side.  The node will override
        // this method if it wants to keep the other side open.
        session.expireServer();
    }

    public void handleUDPServerExpired( NodeUDPSession session )
    {
        // Current assumption: A single expire will be generated on
        // one side of the pipeline, which will travel across it.
        // Another possibility would be to hit them all at once.  Just
        // go ahead and expire the other side.  The node will override
        // this method if it wants to keep the other side open.
        session.expireClient();
    }

    public void handleUDPServerWritable( NodeUDPSession session )
    {
        // do nothing
    }

    public void handleUDPClientWritable( NodeUDPSession session )
    {
        // do nothing
    }

    public void handleUDPFinalized( NodeUDPSession session )
    {
        // do nothing
    }

    public void handleUDPComplete( NodeUDPSession session )
    {
        // do nothing
    }

    public void handleUDPClientPacket( NodeUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        session.sendServerPacket( data, header );
    }

    public void handleUDPServerPacket( NodeUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        session.sendClientPacket( data, header );
    }

}
