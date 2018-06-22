/**
 * $Id$
 */

package com.untangle.uvm;

import java.nio.ByteBuffer;

import com.untangle.jvector.Crumb;
import com.untangle.jvector.IncomingSocketQueue;
import com.untangle.jvector.OutgoingSocketQueue;
import com.untangle.jvector.PacketCrumb;
import com.untangle.jvector.ShutdownCrumb;
import com.untangle.jvector.UDPPacketCrumb;
import com.untangle.uvm.app.SessionEvent;

import com.untangle.uvm.vnet.IPPacketHeader;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.AppUDPSession;
import com.untangle.uvm.vnet.IPStreamer;

/**
 * This is the primary implementation class for UDP live sessions.
 */
public class AppUDPSessionImpl extends AppSessionImpl implements AppUDPSession
{
    protected int[] maxPacketSize;

    protected final byte ttl;
    protected final byte tos;

    private final String logPrefix;

    /**
     * Constructor
     * 
     * @param disp
     *        The dispatcher
     * @param sessionEvent
     *        The session event
     * @param clientMaxPacketSize
     *        Maximum client packet size
     * @param serverMaxPacketSize
     *        Maximum server packet size
     * @param request
     *        The request
     */
    protected AppUDPSessionImpl(Dispatcher disp, SessionEvent sessionEvent, int clientMaxPacketSize, int serverMaxPacketSize, UDPNewSessionRequestImpl request)
    {
        super(disp, sessionEvent, request);

        logPrefix = "UDP" + id();

        if (clientMaxPacketSize < 2 || clientMaxPacketSize > UDP_MAX_MESG_SIZE) throw new IllegalArgumentException("Illegal maximum client packet bufferSize: " + clientMaxPacketSize);
        if (serverMaxPacketSize < 2 || serverMaxPacketSize > UDP_MAX_MESG_SIZE) throw new IllegalArgumentException("Illegal maximum server packet bufferSize: " + serverMaxPacketSize);
        this.maxPacketSize = new int[] { clientMaxPacketSize, serverMaxPacketSize };

        PipelineConnectorImpl pipelineConnector = disp.pipelineConnector();

        this.ttl = request.ttl();
        this.tos = request.tos();
    }

    /**
     * Get the server max packet size
     * 
     * @return The size
     */
    public int serverMaxPacketSize()
    {
        return maxPacketSize[SERVER];
    }

    /**
     * Set the server max packet size
     * 
     * @param numBytes
     *        The size
     */
    public void serverMaxPacketSize(int numBytes)
    {
        if (numBytes < 2 || numBytes > UDP_MAX_MESG_SIZE) throw new IllegalArgumentException("Illegal maximum packet bufferSize: " + numBytes);
        maxPacketSize[SERVER] = numBytes;
    }

    /**
     * Get the client max packet size
     * 
     * @return The size
     */
    public int clientMaxPacketSize()
    {
        return maxPacketSize[CLIENT];
    }

    /**
     * Set the client max packet size
     * 
     * @param numBytes
     *        The size
     */
    public void clientMaxPacketSize(int numBytes)
    {
        if (numBytes < 2 || numBytes > UDP_MAX_MESG_SIZE) throw new IllegalArgumentException("Illegal maximum packet bufferSize: " + numBytes);
        maxPacketSize[CLIENT] = numBytes;
    }

    /**
     * Get the client state
     * 
     * @return The state
     */
    public byte clientState()
    {
        if (clientIncomingSocketQueue() == null) {
            assert clientOutgoingSocketQueue() == null;
            return AppSession.EXPIRED;
        } else {
            assert clientOutgoingSocketQueue() != null;
            return AppSession.OPEN;
        }
    }

    /**
     * Get the server state
     * 
     * @return The state
     */
    public byte serverState()
    {
        if (serverIncomingSocketQueue() == null) {
            assert serverOutgoingSocketQueue() == null;
            return AppSession.EXPIRED;
        } else {
            assert serverOutgoingSocketQueue() != null;
            return AppSession.OPEN;
        }
    }

    /**
     * Expire the server
     */
    public void expireServer()
    {
        OutgoingSocketQueue out = serverOutgoingSocketQueue();
        if (out != null) {
            Crumb crumb = ShutdownCrumb.getInstance(true);
            out.write(crumb);
        }
        // 8/15/05 we also now reset the incoming side, to avoid the race in case a packet outraces
        // the close-other-half event.
        IncomingSocketQueue in = serverIncomingSocketQueue();
        if (in != null) {
            // Should always happen.
            in.reset();
        }
    }

    /**
     * Expire the client
     */
    public void expireClient()
    {
        OutgoingSocketQueue out = clientOutgoingSocketQueue();
        if (out != null) {
            Crumb crumb = ShutdownCrumb.getInstance(true);
            out.write(crumb);
        }
        // 8/15/05 we also now reset the incoming side, to avoid the race in case a packet outraces
        // the close-other-half event.
        IncomingSocketQueue in = clientIncomingSocketQueue();
        if (in != null) {
            // Should always happen.
            in.reset();
        }
    }

    /**
     * Retrieve the TTL for a session, this only has an impact for the last
     * session in the chain when passing data crumbs (UDPPacketCrumbs have TTL
     * value inside of them)
     * 
     * @return The TTL
     */
    public byte ttl()
    {
        return ttl;
    }

    /**
     * Retrieve the TOS for a session, this only has an impact for the last
     * session in the chain when passing data crumbs (UDPPacketCrumbs have TOS
     * value inside of them).
     * 
     * @return The TOS
     */
    public byte tos()
    {
        return tos;
    }

    /**
     * Check if the side is dieing
     * 
     * @param side
     *        The side
     * @param in
     *        The incoming socket queue
     * @return The result
     */
    protected boolean isSideDieing(int side, IncomingSocketQueue in)
    {
        return (in.containsReset() || in.containsShutdown());
    }

    /**
     * Signal side is dieing
     * 
     * @param side
     *        The side
     */
    protected void sideDieing(int side)
    {
        sendExpiredEvent(side);
    }

    /**
     * Send a client packet
     * 
     * @param packet
     *        The packet
     * @param header
     *        The header
     */
    public void sendClientPacket(ByteBuffer packet, IPPacketHeader header)
    {
        sendPacket(CLIENT, packet, header);
    }

    /**
     * Send a server packet
     * 
     * @param packet
     *        The packet
     * @param header
     *        The header
     */
    public void sendServerPacket(ByteBuffer packet, IPPacketHeader header)
    {
        sendPacket(SERVER, packet, header);
    }

    /**
     * Send a packet
     * 
     * @param side
     *        The side
     * @param packet
     *        The packet
     * @param header
     *        The header
     */
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
        addToWriteQueue(side, crumb);
    }

    /**
     * Try writing
     * 
     * @param side
     *        The side
     * @param out
     *        The outgoing socket queue
     * @return The result
     */
    protected boolean tryWrite(int side, OutgoingSocketQueue out)
    {
        if (out == null) {
            logger.error("Invalid arguments.");
            return false;
        }

        if (out.isFull()) {
            logger.warn("tryWrite to full outgoing queue");
            return false;
        }

        Crumb nc = getNextCrumb2Send(side);
        PacketCrumb packet2send = (PacketCrumb) nc;
        assert packet2send != null;
        int numWritten = sendCrumb(packet2send, out);

        if (logger.isDebugEnabled()) {
            logger.debug("wrote " + numWritten + " to " + side);
        }
        return true;
    }

    /**
     * Read a crumb from a streamer
     * 
     * @param streamer
     *        The streamer
     * @return The crumb
     */
    protected Crumb readStreamer(IPStreamer streamer)
    {
        logger.error("Streaming not implemented for UDP", new Exception());
        return null;
    }

    /**
     * Send a writable event
     * 
     * @param side
     *        The side
     */
    protected void sendWritableEvent(int side)
    {
        if (side == CLIENT) dispatcher.dispatchUDPClientWritable(this);
        else dispatcher.dispatchUDPServerWritable(this);
    }

    /**
     * Send a complete event
     */
    protected void sendCompleteEvent()
    {
        dispatcher.dispatchUDPComplete(this);
    }

    /**
     * Send expired event
     * 
     * @param side
     *        The side
     */
    protected void sendExpiredEvent(int side)
    {
        if (side == CLIENT) dispatcher.dispatchUDPClientExpired(this);
        else dispatcher.dispatchUDPServerExpired(this);
    }

    /**
     * Handles the actual reading from the client or server
     * 
     * @param side
     *        The side
     * @param in
     *        The incoming socket queue
     */
    protected void handleRead(int side, IncomingSocketQueue in)
    {
        int numRead = 0;

        assert in != null;
        if (in.isEmpty()) {
            logger.warn("tryReadClient from empty incoming queue");
            return;
        }

        Crumb crumb = in.read();

        switch (crumb.type())
        {
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

        PacketCrumb pc = (PacketCrumb) crumb;
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
        // converted to a DataCrumb on the other side (hence, the next app will fail)

        if (logger.isDebugEnabled()) logger.debug("read " + numRead + " size " + crumb.type() + " packet from " + side);

        // We have received bytes.  Give them to the user.

        // We no longer duplicate the buffer so that the event handler can mess up
        // the position/mark/limit as desired.  This is since the app now sends
        // a buffer manually -- the position and limit must already be correct when sent, so
        // there's no need for us to duplicate here.

        if (side == CLIENT) dispatcher.dispatchUDPClientPacket(this, pbuf, pheader);
        else dispatcher.dispatchUDPServerPacket(this, pbuf, pheader);

        // Nothing more to do, any packets to be sent were queued by called to sendClientPacket(), etc,
        // from app's packet handler.
        return;
    }

    /**
     * Get the id for MDC
     * 
     * @return The string
     */
    @Override
    protected String idForMDC()
    {
        return logPrefix;
    }

    /**
     * Final close
     */
    @Override
    protected void closeFinal()
    {
        try {
            dispatcher.dispatchUDPFinalized(this);
        } catch (Exception x) {
            logger.warn("Exception in Finalized", x);
        }

        super.closeFinal();
    }
}
