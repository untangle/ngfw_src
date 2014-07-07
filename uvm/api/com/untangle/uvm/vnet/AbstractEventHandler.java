/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.vnet.event.SessionEventListener;
import com.untangle.uvm.vnet.event.TCPChunkEvent;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.UDPErrorEvent;
import com.untangle.uvm.vnet.event.UDPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.UDPPacketEvent;
import com.untangle.uvm.vnet.event.UDPSessionEvent;

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

    public void handleTimer( NodeSession session )
    {
    }

    //////////////////////////////////////////////////////////////////////
    // TCP
    //////////////////////////////////////////////////////////////////////

    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event)
    {
        /* accept */
    }

    public void handleTCPNewSession(TCPSessionEvent event)
    {
        /* ignore */
    }

    public void handleTCPClientDataEnd(TCPChunkEvent event)
    {
        /* ignore */
        return;
    }

    public void handleTCPClientFIN(TCPSessionEvent event)
    {
        // Just go ahead and shut down the other side.  The node will override
        // this method if it wants to keep the other side open.
        NodeTCPSession sess = event.session();
        sess.shutdownServer();
    }

    public void handleTCPServerDataEnd(TCPChunkEvent event)
    {
        /* ignore */
        return;
    }

    public void handleTCPServerFIN(TCPSessionEvent event)
    {
        // Just go ahead and shut down the other side.  The node will override
        // this method if it wants to keep the other side open.
        NodeTCPSession sess = event.session();
        sess.shutdownClient();
    }

    public void handleTCPClientRST(TCPSessionEvent event)
    {
        // Just go ahead and reset the other side.  The node will override
        // this method if it wants to keep the other side open.
        NodeTCPSession sess = event.session();
        sess.resetServer();
    }

    public void handleTCPServerRST(TCPSessionEvent event)
    {
        // Just go ahead and reset the other side.  The node will override
        // this method if it wants to keep the other side open.
        NodeTCPSession sess = event.session();
        sess.resetClient();
    }

    public void handleTCPFinalized(TCPSessionEvent event)
    {
    }

    public void handleTCPComplete(TCPSessionEvent event)
    {
    }

    public void handleTCPClientChunk(TCPChunkEvent event)
    {
        NodeTCPSession session = event.session();
        byte serverState = session.serverState();
        // Default just sends the bytes onwards if the output is open.
        if (serverState == NodeTCPSession.OPEN || serverState == NodeTCPSession.HALF_OPEN_OUTPUT)
            session.sendDataToServer( event.data() );
        return;
    }

    public void handleTCPServerChunk(TCPChunkEvent event)
    {
        NodeTCPSession session = event.session();
        byte clientState = session.clientState();
        // Default just sends the bytes onwards if the output is open.
        if (clientState == NodeTCPSession.OPEN || clientState == NodeTCPSession.HALF_OPEN_OUTPUT)
            session.sendDataToClient( event.data() );
        return;
    }

    public void handleTCPServerWritable(TCPSessionEvent event)
    {
        // Default writes nothing more.
        return;
    }

    public void handleTCPClientWritable(TCPSessionEvent event)
    {
        // Default writes nothing more.
        return;
    }

    //////////////////////////////////////////////////////////////////////
    // UDP
    //////////////////////////////////////////////////////////////////////

    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event)
    {
        /* accept */
    }

    public void handleUDPNewSession(UDPSessionEvent event)
    {
        /* ignore */
    }

    public void handleUDPClientExpired(UDPSessionEvent event)
    {
        // Current assumption: A single expire will be generated on
        // one side of the pipeline, which will travel across it.
        // Another possibility would be to hit them all at once.  Just
        // go ahead and expire the other side.  The node will override
        // this method if it wants to keep the other side open.
        NodeUDPSession sess = event.session();
        sess.expireServer();
    }

    public void handleUDPServerExpired(UDPSessionEvent event)
    {
        // Current assumption: A single expire will be generated on
        // one side of the pipeline, which will travel across it.
        // Another possibility would be to hit them all at once.  Just
        // go ahead and expire the other side.  The node will override
        // this method if it wants to keep the other side open.
        NodeUDPSession sess = event.session();
        sess.expireClient();
    }

    public void handleUDPServerWritable(UDPSessionEvent event)
    {
        // Default writes nothing more.
    }

    public void handleUDPClientWritable(UDPSessionEvent event)
    {
        // Default writes nothing more.
    }

    public void handleUDPFinalized(UDPSessionEvent event)
    {
    }

    public void handleUDPComplete(UDPSessionEvent event)
    {
    }

    public void handleUDPClientPacket(UDPPacketEvent event)
    {
        NodeUDPSession session = event.session();
        byte serverState = session.serverState();
        // Default just sends the bytes onwards if the output is open.
        if (serverState == NodeSession.OPEN)
            session.sendServerPacket(event.packet(), event.header());
    }

    public void handleUDPServerPacket(UDPPacketEvent event)
    {
        NodeUDPSession session = event.session();
        byte clientState = session.clientState();
        // Default just sends the bytes onwards if the output is open.
        if (clientState == NodeSession.OPEN)
            session.sendClientPacket(event.packet(), event.header());
    }

}
