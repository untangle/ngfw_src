/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.nio.ByteBuffer;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.vnet.SessionEventListener;

/**
 * <code>AbstractEventHandler</code> is the abstract base class that provides
 * the default actions that any node event handler will need.
 */
public abstract class AbstractEventHandler implements SessionEventListener
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
    }

    //////////////////////////////////////////////////////////////////////
    // TCP
    //////////////////////////////////////////////////////////////////////

    public void handleTCPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        /* accept */
    }

    public void handleTCPNewSession( NodeTCPSession session )
    {
        /* ignore */
    }

    public void handleTCPClientDataEnd( NodeTCPSession session, ByteBuffer data )
    {
        /* ignore */
        return;
    }

    public void handleTCPClientFIN( NodeTCPSession session )
    {
        // Just go ahead and shut down the other side.  The node will override
        // this method if it wants to keep the other side open.
        session.shutdownServer();
    }

    public void handleTCPServerDataEnd( NodeTCPSession session, ByteBuffer data )
    {
        /* ignore */
        return;
    }

    public void handleTCPServerFIN( NodeTCPSession session )
    {
        // Just go ahead and shut down the other side.  The node will override
        // this method if it wants to keep the other side open.
        session.shutdownClient();
    }

    public void handleTCPClientRST( NodeTCPSession session )
    {
        // Just go ahead and reset the other side.  The node will override
        // this method if it wants to keep the other side open.
        session.resetServer();
    }

    public void handleTCPServerRST( NodeTCPSession session )
    {
        // Just go ahead and reset the other side.  The node will override
        // this method if it wants to keep the other side open.
        session.resetClient();
    }

    public void handleTCPFinalized( NodeTCPSession session )
    {
    }

    public void handleTCPComplete( NodeTCPSession session )
    {
    }

    public void handleTCPClientChunk( NodeTCPSession session, ByteBuffer data )
    {
        byte serverState = session.serverState();
        // Default just sends the bytes onwards if the output is open.
        if (serverState == NodeTCPSession.OPEN || serverState == NodeTCPSession.HALF_OPEN_OUTPUT)
            session.sendDataToServer( data );
        return;
    }

    public void handleTCPServerChunk( NodeTCPSession session, ByteBuffer data )
    {
        byte clientState = session.clientState();
        // Default just sends the bytes onwards if the output is open.
        if (clientState == NodeTCPSession.OPEN || clientState == NodeTCPSession.HALF_OPEN_OUTPUT)
            session.sendDataToClient( data );
        return;
    }

    public void handleTCPClientObject( NodeTCPSession session, Object obj )
    {
        byte serverState = session.serverState();
        // Default just sends the bytes onwards if the output is open.
        if (serverState == NodeTCPSession.OPEN || serverState == NodeTCPSession.HALF_OPEN_OUTPUT)
            session.sendObjectToServer( obj );
        return;
    }

    public void handleTCPServerObject( NodeTCPSession session, Object obj )
    {
        byte clientState = session.clientState();
        // Default just sends the bytes onwards if the output is open.
        if (clientState == NodeTCPSession.OPEN || clientState == NodeTCPSession.HALF_OPEN_OUTPUT)
            session.sendObjectToClient( obj );
        return;
    }
    
    public void handleTCPServerWritable( NodeTCPSession session )
    {
        // Default writes nothing more.
        return;
    }

    public void handleTCPClientWritable( NodeTCPSession session )
    {
        // Default writes nothing more.
        return;
    }

    //////////////////////////////////////////////////////////////////////
    // UDP
    //////////////////////////////////////////////////////////////////////

    public void handleUDPNewSessionRequest( UDPNewSessionRequest sessionRequest )
    {
        /* accept */
    }

    public void handleUDPNewSession( NodeUDPSession session )
    {
        /* ignore */
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
        // Default writes nothing more.
    }

    public void handleUDPClientWritable( NodeUDPSession session )
    {
        // Default writes nothing more.
    }

    public void handleUDPFinalized( NodeUDPSession session )
    {
    }

    public void handleUDPComplete( NodeUDPSession session )
    {
    }

    public void handleUDPClientPacket( NodeUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        byte serverState = session.serverState();
        // Default just sends the bytes onwards if the output is open.
        if (serverState == NodeSession.OPEN)
            session.sendServerPacket( data, header );
    }

    public void handleUDPServerPacket( NodeUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        byte clientState = session.clientState();
        // Default just sends the bytes onwards if the output is open.
        if (clientState == NodeSession.OPEN)
            session.sendClientPacket( data, header );
    }

}
