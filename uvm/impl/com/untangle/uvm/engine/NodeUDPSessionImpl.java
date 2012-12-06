/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.net.InetAddress;
import java.nio.ByteBuffer;

import com.untangle.jvector.Crumb;
import com.untangle.jvector.IncomingSocketQueue;
import com.untangle.jvector.OutgoingSocketQueue;
import com.untangle.jvector.PacketCrumb;
import com.untangle.jvector.ShutdownCrumb;
import com.untangle.jvector.UDPPacketCrumb;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.argon.ArgonUDPSession;
import com.untangle.uvm.util.MetaEnv;
import com.untangle.uvm.vnet.IPPacketHeader;
import com.untangle.uvm.vnet.NodeSessionStats;
import com.untangle.uvm.vnet.NodeIPSession;
import com.untangle.uvm.vnet.NodeUDPSession;
import com.untangle.uvm.node.SessionTuple;
import com.untangle.uvm.vnet.event.IPStreamer;
import com.untangle.uvm.vnet.event.UDPErrorEvent;
import com.untangle.uvm.vnet.event.UDPPacketEvent;
import com.untangle.uvm.vnet.event.UDPSessionEvent;

/**
 * This is the primary implementation class for UDP live sessions.
 */
class NodeUDPSessionImpl extends NodeIPSessionImpl implements NodeUDPSession
{
    protected int[] maxPacketSize;

    private final String logPrefix;
    
    protected NodeUDPSessionImpl(Dispatcher disp,
                             ArgonUDPSession argonSession,
                             SessionEvent pe,
                             int clientMaxPacketSize,
                             int serverMaxPacketSize)
    {
        super(disp, argonSession, pe);

        logPrefix = "UDP" + id();
        
        if (clientMaxPacketSize < 2 || clientMaxPacketSize > UDP_MAX_MESG_SIZE)
            throw new IllegalArgumentException("Illegal maximum client packet bufferSize: " + clientMaxPacketSize);
        if (serverMaxPacketSize < 2 || serverMaxPacketSize > UDP_MAX_MESG_SIZE)
            throw new IllegalArgumentException("Illegal maximum server packet bufferSize: " + serverMaxPacketSize);
        this.maxPacketSize = new int[] { clientMaxPacketSize, serverMaxPacketSize };

        ArgonConnectorImpl argonConnector = disp.argonConnector();

        logger = argonConnector.sessionLoggerUDP();
    }

    public int serverMaxPacketSize()
    {
        return maxPacketSize[SERVER];
    }

    public void serverMaxPacketSize(int numBytes)
    {
        if (numBytes < 2 || numBytes > UDP_MAX_MESG_SIZE)
            throw new IllegalArgumentException("Illegal maximum packet bufferSize: " + numBytes);
        maxPacketSize[SERVER] = numBytes;
    }

    public int clientMaxPacketSize()
    {
        return maxPacketSize[CLIENT];
    }

    public void clientMaxPacketSize(int numBytes)
    {
        if (numBytes < 2 || numBytes > UDP_MAX_MESG_SIZE)
            throw new IllegalArgumentException("Illegal maximum packet bufferSize: " + numBytes);
        maxPacketSize[CLIENT] = numBytes;
    }

    public byte clientState()
    {
        if ((argonSession).clientIncomingSocketQueue() == null) {
            assert (argonSession).clientOutgoingSocketQueue() == null;
            return NodeIPSession.EXPIRED;
        } else {
            assert (argonSession).clientOutgoingSocketQueue() != null;
            return NodeIPSession.OPEN;
        }
    }

    public byte serverState()
    {
        if ((argonSession).serverIncomingSocketQueue() == null) {
            assert (argonSession).serverOutgoingSocketQueue() == null;
            return NodeIPSession.EXPIRED;
        } else {
            assert (argonSession).serverOutgoingSocketQueue() != null;
            return NodeIPSession.OPEN;
        }
    }

    public void expireServer()
    {
        OutgoingSocketQueue out = (argonSession).serverOutgoingSocketQueue();
        if (out != null) {
            Crumb crumb = ShutdownCrumb.getInstance(true);
            out.write(crumb);
        }
        // 8/15/05 we also now reset the incoming side, to avoid the race in case a packet outraces
        // the close-other-half event.
        IncomingSocketQueue in = (argonSession).serverIncomingSocketQueue();
        if (in != null) {
            // Should always happen.
            in.reset();
        }
    }

    public void expireClient()
    {
        OutgoingSocketQueue out = (argonSession).clientOutgoingSocketQueue();
        if (out != null) {
            Crumb crumb = ShutdownCrumb.getInstance(true);
            out.write(crumb);
        }
        // 8/15/05 we also now reset the incoming side, to avoid the race in case a packet outraces
        // the close-other-half event.
        IncomingSocketQueue in = (argonSession).clientIncomingSocketQueue();
        if (in != null) {
            // Should always happen.
            in.reset();
        }
    }

    protected boolean isSideDieing(int side, IncomingSocketQueue in)
    {
        return (in.containsReset() || in.containsShutdown());
    }

    protected void sideDieing(int side)
        
    {
        sendExpiredEvent(side);
    }

    public void sendClientPacket(ByteBuffer packet, IPPacketHeader header)
    {
        sendPacket(CLIENT, packet, header);
    }

    public void sendServerPacket(ByteBuffer packet, IPPacketHeader header)
    {
        sendPacket(SERVER, packet, header);
    }

    private void sendPacket(int side, ByteBuffer packet, IPPacketHeader header)
    {
        byte[] array;
        int offset = packet.position();
        int limit = packet.remaining();
        if (packet.hasArray()) {
            array = packet.array();
            offset += packet.arrayOffset();
            limit += packet.arrayOffset();
        } else {
            logger.warn("out-of-help byte buffer, had to copy");
            array = new byte[packet.remaining()];
            packet.get(array);
            packet.position(offset);
            offset = 0;
        }

        UDPPacketCrumb crumb = new UDPPacketCrumb(header.ttl(), header.tos(), header.options(), array, offset, limit);
        addCrumb(side, crumb);
    }

    protected void tryWrite(int side, OutgoingSocketQueue out, boolean warnIfUnable)
        
    {
        assert out != null;
        if (out.isFull()) {
            if (warnIfUnable)
                logger.warn("tryWrite to full outgoing queue");
            else
                logger.debug("tryWrite to full outgoing queue");
        } else {
            Crumb nc = getNextCrumb2Send(side);
            PacketCrumb packet2send = (PacketCrumb) nc;
            assert packet2send != null;
            int numWritten = sendCrumb(packet2send, out);

            stats.wroteData(side, numWritten);

            if (logger.isDebugEnabled()) {
                logger.debug("wrote " + numWritten + " to " + side);
            }
        }
    }

    protected void addStreamBuf(int side, IPStreamer ipStreamer)
        
    {

        /* Not Yet supported
           UDPStreamer streamer = (UDPStreamer)ipStreamer;

           String sideName = (side == CLIENT ? "client" : "server");

           ByteBuffer packet2send = streamer.nextPacket();
           if (packet2send == null) {
           logger.debug("end of stream");
           streamer = null;
           return;
           }

           // Ug. XXX
           addBuf(side, packet2send);

           if (logger.isDebugEnabled())
           logger.debug("streamed " + packet2send.remaining() + " to " + sideName);
        */
    }

    protected void sendWritableEvent(int side)
        
    {
        UDPSessionEvent wevent = new UDPSessionEvent(argonConnector, this);
        if (side == CLIENT)
            dispatcher.dispatchUDPClientWritable(wevent);
        else
            dispatcher.dispatchUDPServerWritable(wevent);
    }

    protected void sendCompleteEvent()
        
    {
        UDPSessionEvent wevent = new UDPSessionEvent(argonConnector, this);
        dispatcher.dispatchUDPComplete(wevent);
    }

    protected void sendExpiredEvent(int side)
        
    {
        UDPSessionEvent wevent = new UDPSessionEvent(argonConnector, this);
        if (side == CLIENT)
            dispatcher.dispatchUDPClientExpired(wevent);
        else
            dispatcher.dispatchUDPServerExpired(wevent);
    }

    // Handles the actual reading from the client
    protected void tryRead(int side, IncomingSocketQueue in, boolean warnIfUnable)
        
    {
        int numRead = 0;

        assert in != null;
        if (in.isEmpty()) {
            if (warnIfUnable)
                logger.warn("tryReadClient from empty incoming queue");
            else
                logger.debug("tryReadClient from empty incoming queue");
            return;
        }

        Crumb crumb = in.read();

        switch (crumb.type()) {
        case Crumb.TYPE_SHUTDOWN:
        case Crumb.TYPE_RESET:
        case Crumb.TYPE_DATA:
            // Should never happen (TCP).
            logger.debug("udp read crumb " + crumb.type());
            assert false;
            break;
        default:
            // Now we know this is a UDP.
        }

        PacketCrumb pc = (PacketCrumb)crumb;
        IPPacketHeader pheader = new IPPacketHeader(pc.ttl(), pc.tos(), pc.options());
        byte[] pcdata = pc.data();
        int pclimit = pc.limit();
        //int pccap = pcdata.length;
        int pcoffset = pc.offset();
        int pcsize = pclimit - pcoffset;
        if (pcoffset >= pclimit) {
            logger.warn("Zero length UDP crumb read");
            return;
        }
        ByteBuffer pbuf;
        if (pcoffset != 0) {
            // XXXX
            assert false;
            pbuf = null;
        } else {
            pbuf = ByteBuffer.wrap(pcdata, 0, pcsize);
            numRead = pcsize;
        }

        // Wrap a byte buffer around the data.
        // XXX This may or may not be a UDP crumb depending on what gets passed.
        // Right now just always do DataCrumbs, since a UDPPacketCrumb coming in just gets
        // converted to a DataCrumb on the other side (hence, the next node will fail)

        if (logger.isDebugEnabled())
            logger.debug("read " + numRead + " size " + crumb.type() + " packet from " + side);

        stats.readData(side, numRead);

        // We have received bytes.  Give them to the user.

        // We no longer duplicate the buffer so that the event handler can mess up
        // the position/mark/limit as desired.  This is since the node now sends
        // a buffer manually -- the position and limit must already be correct when sent, so
        // there's no need for us to duplicate here.

        UDPPacketEvent event = new UDPPacketEvent(argonConnector, this, pbuf, pheader);
        if (side == CLIENT)
            dispatcher.dispatchUDPClientPacket(event);
        else
            dispatcher.dispatchUDPServerPacket(event);

        // Nothing more to do, any packets to be sent were queued by called to sendClientPacket(), etc,
        // from node's packet handler.
    }

    @Override
    protected String idForMDC()
    {
        return logPrefix;
    }

    @Override
    protected void closeFinal()
    {
        try {
            UDPSessionEvent wevent = new UDPSessionEvent(argonConnector, this);
            dispatcher.dispatchUDPFinalized(wevent);
        } catch (Exception x) {
            logger.warn("Exception in Finalized", x);
        }

        super.closeFinal();
    }

    @Override
    protected void killSession(String reason)
    {
        // Sends a RST both directions and nukes the socket queues.
        argonSession.killSession();
    }

    // Don't need equal or hashcode since we can only have one of these objects per
    // session (so the memory address is ok for equals/hashcode).
}






