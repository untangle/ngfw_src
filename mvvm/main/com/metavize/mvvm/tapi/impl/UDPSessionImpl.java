/*
 * Copyright (c) 2003-2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: UDPSessionImpl.java,v 1.25 2005/03/22 00:01:27 jdi Exp $
 */

package com.metavize.mvvm.tapi.impl;

import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tran.MutateTStats;
import com.metavize.mvvm.tapi.event.*;
import com.metavize.mvvm.util.MetaEnv;

import com.metavize.mvvm.tapi.client.UDPSessionDescImpl;
import com.metavize.jnetcap.UDPPacket;
import com.metavize.jvector.IncomingSocketQueue;
import com.metavize.jvector.OutgoingSocketQueue;
import com.metavize.jvector.Crumb;
import com.metavize.jvector.UDPPacketCrumb;
import com.metavize.jvector.DataCrumb;
import com.metavize.jvector.ShutdownCrumb;
import com.metavize.jvector.ResetCrumb;
import com.metavize.jvector.SocketQueueListener;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import org.apache.log4j.Level;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import org.apache.log4j.Logger;

class UDPSessionImpl extends IPSessionImpl implements UDPSession
{
    protected int[] maxPacketSize;

    protected UDPSessionImpl(Dispatcher disp,
                             com.metavize.mvvm.argon.UDPSession pSession,
                             int clientMaxPacketSize,
                             int serverMaxPacketSize)
    {
        super(disp, pSession);

        if (clientMaxPacketSize < 2 || clientMaxPacketSize > UDP_MAX_MESG_SIZE)
            throw new IllegalArgumentException("Illegal maximum client packet bufferSize: " + clientMaxPacketSize);
        if (serverMaxPacketSize < 2 || serverMaxPacketSize > UDP_MAX_MESG_SIZE)
            throw new IllegalArgumentException("Illegal maximum server packet bufferSize: " + serverMaxPacketSize);
        this.maxPacketSize = new int[] { clientMaxPacketSize, serverMaxPacketSize };
        logger = disp.mPipe().sessionLoggerUDP();
    }

    public int serverMaxPacketSize() {
        return maxPacketSize[SERVER];
    }
    public void serverMaxPacketSize(int numBytes) {
        if (numBytes < 2 || numBytes > UDP_MAX_MESG_SIZE)
            throw new IllegalArgumentException("Illegal maximum packet bufferSize: " + numBytes);
        maxPacketSize[SERVER] = numBytes;
    }

    public int clientMaxPacketSize() {
        return maxPacketSize[CLIENT];
    }
    public void clientMaxPacketSize(int numBytes) {
        if (numBytes < 2 || numBytes > UDP_MAX_MESG_SIZE)
            throw new IllegalArgumentException("Illegal maximum packet bufferSize: " + numBytes);
        maxPacketSize[CLIENT] = numBytes;
    }

    public IPSessionDesc makeDesc()
    {
        return new UDPSessionDescImpl(id(), new SessionStats(stats), clientState(), serverState(),
                                      clientIntf(), serverIntf(),
                                      clientAddr(), serverAddr(), clientPort(), serverPort());
    }

    public byte clientState()
    {
        if (((com.metavize.mvvm.argon.Session)pSession).clientIncomingSocketQueue() == null) {
            assert ((com.metavize.mvvm.argon.Session)pSession).clientOutgoingSocketQueue() == null;
            return IPSessionDesc.EXPIRED;
        } else {
            assert ((com.metavize.mvvm.argon.Session)pSession).clientOutgoingSocketQueue() != null;
            return IPSessionDesc.OPEN;
        }
    }

    public byte serverState()
    {
        if (((com.metavize.mvvm.argon.Session)pSession).serverIncomingSocketQueue() == null) {
            assert ((com.metavize.mvvm.argon.Session)pSession).serverOutgoingSocketQueue() == null;
            return IPSessionDesc.EXPIRED;
        } else {
            assert ((com.metavize.mvvm.argon.Session)pSession).serverOutgoingSocketQueue() != null;
            return IPSessionDesc.OPEN;
        }
    }

    public void expireServer()
    {
        OutgoingSocketQueue out = ((com.metavize.mvvm.argon.Session)pSession).serverOutgoingSocketQueue();
        if (out != null) {
            Crumb crumb = ShutdownCrumb.getInstance();
            boolean success = out.write(crumb);
            assert success;
        }
    }

    public void expireClient()
    {
        OutgoingSocketQueue out = ((com.metavize.mvvm.argon.Session)pSession).clientOutgoingSocketQueue();
        if (out != null) {
            Crumb crumb = ShutdownCrumb.getInstance();
            boolean success = out.write(crumb);
            assert success;
        }
    }

    protected boolean sideDieing(int side, IncomingSocketQueue in)
        throws MPipeException
    {
        if (in.containsShutdown()) {
            sendExpiredEvent(side);
            return true;
        }
        return false;
    }

    void tryWrite(int side, OutgoingSocketQueue out, boolean warnIfUnable)
        throws MPipeException
    {
        String sideName = (side == CLIENT ? "client" : "server");
        assert out != null;
        if (out.isFull()) {
            if (warnIfUnable)
                warn("tryWrite to full outgoing queue");
            else
                debug("tryWrite to full outgoing queue");
        } else {
            ByteBuffer packet2send = getNextBuf2Send(side);
            assert packet2send != null;
            int numWritten = sendPacket(packet2send, out);
            if (RWSessionStats.DoDetailedTimes) {
                long[] times = stats.times();
                if (times[SessionStats.FIRST_BYTE_WROTE_TO_CLIENT + side] == 0)
                    times[SessionStats.FIRST_BYTE_WROTE_TO_CLIENT + side] = MetaEnv.currentTimeMillis();
            }
            mPipe.lastSessionWriteFailed(false);
            stats.wroteData(side, numWritten);
            MutateTStats.wroteData(side, this, numWritten);
            if (logger.isDebugEnabled())
                debug("wrote " + packet2send.remaining() + " to " + side);
        }
    }

    void addStreamBuf(int side, IPStreamer ipStreamer)
        throws MPipeException
    {
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
    }

    private int sendPacket(ByteBuffer packet2send, OutgoingSocketQueue out)
    {
        Crumb crumb;
        byte[] array = null;
        int size = packet2send.remaining();
        int offset = packet2send.position();
        if (packet2send.hasArray()) {
            array = packet2send.array();
            offset += packet2send.arrayOffset();
        } else {
            warn("had to copy");
            array = new byte[packet2send.remaining()];
            packet2send.get(array);
            packet2send.position(offset);
            offset = 0;
        }
        crumb = new DataCrumb(array, offset, size);
        boolean success = out.write(crumb);
        assert success;
        return size;
    }

    protected void sendWritableEvent(int side)
        throws MPipeException
    {
        UDPSessionEvent wevent = new UDPSessionEvent(mPipe, this);
        IPDataResult result = side == CLIENT ? dispatcher.dispatchUDPClientWritable(wevent)
            : dispatcher.dispatchUDPServerWritable(wevent);
        if (result == IPDataResult.SEND_NOTHING)
            // Optimization
            return;
        if (result.readBuffer() != null)
            // Not allowed
            warn("Ignoring readBuffer returned from writable event");
        addBufs(CLIENT, result.bufsToClient());
        addBufs(SERVER, result.bufsToServer());
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
        String sideName = (side == CLIENT ? "client" : "server");

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
            // Should never happen.
            debug("udp read Expire");
            assert false;
            return;
        case Crumb.TYPE_RESET:
            // Should never happen.
            debug("udp read RST");
            assert false;
            break;
        default:
        case Crumb.TYPE_DATA:
            break;
        }
            
        // Wrap a byte buffer around the data.
        // XXX This may or may not be a UDP crumb depending on what gets passed.
        // Right now just always do DataCrumbs, since a UDPPacketCrumb coming in just gets
        // converted to a DataCrumb on the other side (hence, the next transform will fail)
        DataCrumb pc = (DataCrumb)crumb;
        byte[] pcdata = pc.data();
        int pclimit = pc.limit();
        int pccap = pcdata.length;
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

        if (logger.isDebugEnabled())
            debug("read " + numRead + " size packet from " + side);
        dispatcher.lastSessionNumRead(numRead);

        stats.readData(side, numRead);
        MutateTStats.readData(side, this, numRead);
            
        // We have received bytes.  Give them to the user.

        // We duplicate the buffer so that the event handler can mess up
        // the position/mark/limit as desired.
        ByteBuffer userPacket;
        /*
        if (readOnly())
            userPacket = pbuf.asReadOnlyBuffer();
        else
        */
        userPacket = pbuf.duplicate();
        IPDataResult result;
        UDPPacketEvent event = new UDPPacketEvent(mPipe, this, userPacket);
        if (side == CLIENT)
            result = dispatcher.dispatchUDPClientPacket(event);
        else
            result = dispatcher.dispatchUDPServerPacket(event);
        if (/* readOnly() || */ result == IPDataResult.PASS_THROUGH) {
            // Send it to the other side.
            addBuf(1 - side, pbuf);
        } else if (result == TCPChunkResult.READ_MORE_NO_WRITE) {
            assert false;
        } else {
            addBufs(CLIENT, result.bufsToClient());
            addBufs(SERVER, result.bufsToServer());
            assert result.readBuffer() == null;
        }
    }

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

        MutateTStats.removeUDPSession(mPipe);
        super.closeFinal();
    }

    protected void killSession(String reason)
    {   
        // Sends a RST both directions and nukes the socket queues.
        pSession.killSession();
    } 
        
    StringBuffer logPrefix()
    {
        StringBuffer logPrefix = new StringBuffer("<U");
        logPrefix.append(id());
        logPrefix.append("> (");
        logPrefix.append(Thread.currentThread().getName());
        logPrefix.append("): ");
        return logPrefix;
    }

    // Don't need equal or hashcode since we can only have one of these objects per
    // session (so the memory address is ok for equals/hashcode).
}



                

    
