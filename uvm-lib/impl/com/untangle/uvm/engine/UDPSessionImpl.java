/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.net.InetAddress;
import java.nio.ByteBuffer;

import com.untangle.jvector.Crumb;
import com.untangle.jvector.ICMPPacketCrumb;
import com.untangle.jvector.IncomingSocketQueue;
import com.untangle.jvector.OutgoingSocketQueue;
import com.untangle.jvector.PacketCrumb;
import com.untangle.jvector.ShutdownCrumb;
import com.untangle.jvector.UDPPacketCrumb;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.message.BlingBlinger;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.LocalMessageManager;
import com.untangle.uvm.node.PipelineEndpoints;
import com.untangle.uvm.util.MetaEnv;
import com.untangle.uvm.vnet.IPPacketHeader;
import com.untangle.uvm.vnet.IPSessionDesc;
import com.untangle.uvm.vnet.MPipeException;
import com.untangle.uvm.vnet.SessionStats;
import com.untangle.uvm.vnet.UDPSession;
import com.untangle.uvm.vnet.client.UDPSessionDescImpl;
import com.untangle.uvm.vnet.event.IPStreamer;
import com.untangle.uvm.vnet.event.UDPErrorEvent;
import com.untangle.uvm.vnet.event.UDPPacketEvent;
import com.untangle.uvm.vnet.event.UDPSessionEvent;

/**
 * This is the primary implementation class for UDP live sessions.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
class UDPSessionImpl extends IPSessionImpl implements UDPSession
{
    protected int[] maxPacketSize;

    private final BlingBlinger s2nChunks;
    private final BlingBlinger c2nChunks;
    private final BlingBlinger n2sChunks;
    private final BlingBlinger n2cChunks;
    private final BlingBlinger s2nBytes;
    private final BlingBlinger c2nBytes;
    private final BlingBlinger n2sBytes;
    private final BlingBlinger n2cBytes;

    protected UDPSessionImpl(Dispatcher disp,
                             com.untangle.uvm.argon.UDPSession argonSession,
                             PipelineEndpoints pe,
                             int clientMaxPacketSize,
                             int serverMaxPacketSize)
    {
        super(disp, argonSession, pe);

        if (clientMaxPacketSize < 2 || clientMaxPacketSize > UDP_MAX_MESG_SIZE)
            throw new IllegalArgumentException("Illegal maximum client packet bufferSize: " + clientMaxPacketSize);
        if (serverMaxPacketSize < 2 || serverMaxPacketSize > UDP_MAX_MESG_SIZE)
            throw new IllegalArgumentException("Illegal maximum server packet bufferSize: " + serverMaxPacketSize);
        this.maxPacketSize = new int[] { clientMaxPacketSize, serverMaxPacketSize };

        MPipeImpl mPipe = disp.mPipe();

        logger = mPipe.sessionLoggerUDP();

        LocalMessageManager lmm = LocalUvmContextFactory.context()
            .localMessageManager();
        Counters c = lmm.getCounters(mPipe.node().getTid());
        s2nChunks = c.getBlingBlinger("s2nChunks");
        c2nChunks = c.getBlingBlinger("c2nChunks");
        n2sChunks = c.getBlingBlinger("n2sChunks");
        n2cChunks = c.getBlingBlinger("n2cChunks");
        s2nBytes = c.getBlingBlinger("s2nBytes");
        c2nBytes = c.getBlingBlinger("c2nBytes");
        n2sBytes = c.getBlingBlinger("n2sBytes");
        n2cBytes = c.getBlingBlinger("n2cBytes");
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

    /**
     * Returns true if this is a Ping session
     */
    public boolean isPing()
    {
        return ((com.untangle.uvm.argon.UDPSession)argonSession).isPing();
    }

    public int icmpId()
    {
        return ((com.untangle.uvm.argon.UDPSession)argonSession).icmpId();
    }

    public IPSessionDesc makeDesc()
     {
        return new UDPSessionDescImpl(id(), new SessionStats(stats),
                                      clientState(), serverState(),
                                      clientIntf(), serverIntf(), clientAddr(),
                                      serverAddr(), clientPort(), serverPort());
    }

    public byte clientState()
    {
        if ((argonSession).clientIncomingSocketQueue() == null) {
            assert (argonSession).clientOutgoingSocketQueue() == null;
            return IPSessionDesc.EXPIRED;
        } else {
            assert (argonSession).clientOutgoingSocketQueue() != null;
            return IPSessionDesc.OPEN;
        }
    }

    public byte serverState()
    {
        if ((argonSession).serverIncomingSocketQueue() == null) {
            assert (argonSession).serverOutgoingSocketQueue() == null;
            return IPSessionDesc.EXPIRED;
        } else {
            assert (argonSession).serverOutgoingSocketQueue() != null;
            return IPSessionDesc.OPEN;
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
        throws MPipeException
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
            warn("out-of-help byte buffer, had to copy");
            array = new byte[packet.remaining()];
            packet.get(array);
            packet.position(offset);
            offset = 0;
        }

        UDPPacketCrumb crumb = new UDPPacketCrumb(header.ttl(), header.tos(), header.options(),
                                                  array, offset, limit);
        addCrumb(side, crumb);
    }

    public void sendClientError(byte icmpType, byte icmpCode, ByteBuffer icmpData, InetAddress icmpSource, IPPacketHeader header)
    {
        sendError(CLIENT, icmpType, icmpCode, icmpData, icmpSource, header);
    }

    public void sendServerError(byte icmpType, byte icmpCode, ByteBuffer icmpData, InetAddress icmpSource, IPPacketHeader header)
    {
        sendError(SERVER, icmpType, icmpCode, icmpData, icmpSource, header);
    }

    private void sendError(int side, byte icmpType, byte icmpCode, ByteBuffer icmpData, InetAddress icmpSource, IPPacketHeader header)
    {
        byte[] array;
        int offset = icmpData.position();
        int limit = icmpData.remaining();
        if (icmpData.hasArray()) {
            array = icmpData.array();
            offset += icmpData.arrayOffset();
            limit += icmpData.arrayOffset();
        } else {
            warn("out-of-help byte buffer, had to copy");
            array = new byte[icmpData.remaining()];
            icmpData.get(array);
            icmpData.position(offset);
            offset = 0;
        }
        ICMPPacketCrumb crumb = new ICMPPacketCrumb(header.ttl(), header.tos(), header.options(),
                                                    icmpType, icmpCode, icmpSource, array, offset, limit);
        addCrumb(side, crumb);
    }

    void tryWrite(int side, OutgoingSocketQueue out, boolean warnIfUnable)
        throws MPipeException
    {
        assert out != null;
        if (out.isFull()) {
            if (warnIfUnable)
                warn("tryWrite to full outgoing queue");
            else
                debug("tryWrite to full outgoing queue");
        } else {
            // Note: This can be an ICMP or UDP packet.
            Crumb nc = getNextCrumb2Send(side);
            PacketCrumb packet2send = (PacketCrumb) nc;
            assert packet2send != null;
            int numWritten = sendCrumb(packet2send, out);
            if (RWSessionStats.DoDetailedTimes) {
                long[] times = stats.times();
                if (times[SessionStats.FIRST_BYTE_WROTE_TO_CLIENT + side] == 0)
                    times[SessionStats.FIRST_BYTE_WROTE_TO_CLIENT + side] = MetaEnv.currentTimeMillis();
            }
            mPipe.lastSessionWriteFailed(false);

            stats.wroteData(side, numWritten);

            if (SERVER == side) {
                n2sChunks.increment();
                n2sBytes.increment(numWritten);
            } else {
                n2cChunks.increment();
                n2cBytes.increment(numWritten);
            }

            if (logger.isDebugEnabled()) {
                debug("wrote " + numWritten + " to " + side);
            }
        }
    }

    void addStreamBuf(int side, IPStreamer ipStreamer)
        throws MPipeException
    {

        /* Not Yet supported
           UDPStreamer streamer = (UDPStreamer)ipStreamer;

           String sideName = (side == CLIENT ? "client" : "server");

           ByteBuffer packet2send = streamer.nextPacket();
           if (packet2send == null) {
           debug("end of stream");
           streamer = null;
           return;
           }

           // Ug. XXX
           addBuf(side, packet2send);

           if (logger.isDebugEnabled())
           debug("streamed " + packet2send.remaining() + " to " + sideName);
        */
    }

    protected void sendWritableEvent(int side)
        throws MPipeException
    {
        UDPSessionEvent wevent = new UDPSessionEvent(mPipe, this);
        if (side == CLIENT)
            dispatcher.dispatchUDPClientWritable(wevent);
        else
            dispatcher.dispatchUDPServerWritable(wevent);
    }

    protected void sendCompleteEvent()
        throws MPipeException
    {
        UDPSessionEvent wevent = new UDPSessionEvent(mPipe, this);
        dispatcher.dispatchUDPComplete(wevent);
    }

    protected void sendExpiredEvent(int side)
        throws MPipeException
    {
        UDPSessionEvent wevent = new UDPSessionEvent(mPipe, this);
        if (side == CLIENT)
            dispatcher.dispatchUDPClientExpired(wevent);
        else
            dispatcher.dispatchUDPServerExpired(wevent);
    }

    // Handles the actual reading from the client
    void tryRead(int side, IncomingSocketQueue in, boolean warnIfUnable)
        throws MPipeException
    {
        int numRead = 0;

        assert in != null;
        if (in.isEmpty()) {
            if (warnIfUnable)
                warn("tryReadClient from empty incoming queue");
            else
                debug("tryReadClient from empty incoming queue");
            return;
        }

        Crumb crumb = in.read();
        if (RWSessionStats.DoDetailedTimes) {
            long[] times = stats.times();
            if (times[SessionStats.FIRST_BYTE_READ_FROM_CLIENT + side] == 0)
                times[SessionStats.FIRST_BYTE_READ_FROM_CLIENT + side] = MetaEnv.currentTimeMillis();
        }

        switch (crumb.type()) {
        case Crumb.TYPE_SHUTDOWN:
        case Crumb.TYPE_RESET:
        case Crumb.TYPE_DATA:
            // Should never happen (TCP).
            debug("udp read crumb " + crumb.type());
            assert false;
            break;
        default:
            // Now we know either UDP or ICMP packet.
        }

        PacketCrumb pc = (PacketCrumb)crumb;
        IPPacketHeader pheader = new IPPacketHeader(pc.ttl(), pc.tos(), pc.options());
        byte[] pcdata = pc.data();
        int pclimit = pc.limit();
        //int pccap = pcdata.length;
        int pcoffset = pc.offset();
        int pcsize = pclimit - pcoffset;
        if (pcoffset >= pclimit) {
            warn("Zero length UDP crumb read");
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
            debug("read " + numRead + " size " + crumb.type() + " packet from " + side);

        stats.readData(side, numRead);

        if (CLIENT == side) {
            c2nChunks.increment();
            c2nBytes.increment(numRead);
        } else {
            s2nChunks.increment();
            s2nBytes.increment(numRead);
        }

        // We have received bytes.  Give them to the user.

        // We no longer duplicate the buffer so that the event handler can mess up
        // the position/mark/limit as desired.  This is since the node now sends
        // a buffer manually -- the position and limit must already be correct when sent, so
        // there's no need for us to duplicate here.

        if (crumb.type() == Crumb.TYPE_ICMP_PACKET) {
            ICMPPacketCrumb icrumb = (ICMPPacketCrumb)crumb;
            byte icmpType = icrumb.icmpType();
            byte icmpCode = icrumb.icmpCode();
            InetAddress source = icrumb.source();
            UDPErrorEvent event = new UDPErrorEvent(mPipe, this, pbuf, pheader, icmpType, icmpCode, source);
            if (side == CLIENT)
                dispatcher.dispatchUDPClientError(event);
            else
                dispatcher.dispatchUDPServerError(event);
        } else {
            UDPPacketEvent event = new UDPPacketEvent(mPipe, this, pbuf, pheader);
            if (side == CLIENT)
                dispatcher.dispatchUDPClientPacket(event);
            else
                dispatcher.dispatchUDPServerPacket(event);
        }
        // Nothing more to do, any packets to be sent were queued by called to sendClientPacket(), etc,
        // from node's packet handler.
    }

    @Override
    String idForMDC()
    {
        StringBuilder logPrefix = new StringBuilder("U");
        logPrefix.append(id());
        return logPrefix.toString();
    }

    @Override
    protected void closeFinal()
    {
        try {
            UDPSessionEvent wevent = new UDPSessionEvent(mPipe, this);
            dispatcher.dispatchUDPFinalized(wevent);
        } catch (MPipeException x) {
            warn("MPipeException in Finalized", x);
        } catch (Exception x) {
            warn("Exception in Finalized", x);
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






