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
package com.untangle.node.test;

import java.nio.*;

import com.untangle.uvm.vnet.*;
import com.untangle.uvm.vnet.event.*;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.util.MetaEnv;
import com.untangle.uvm.util.SessionUtil;
import org.apache.log4j.Logger;

public class EventHandler extends AbstractEventHandler
{
    private static final long HALF_OPEN_TIMEOUT = 10000; // 10 seconds

    private final Logger logger = Logger.getLogger(getClass());

    private TestSettings settings;

    public EventHandler(Node node, TestSettings settings)
    {
        super(node);

        if (settings == null) {
            throw new IllegalArgumentException("No settings");
        }

        this.settings = settings;
    }

    void setSettings(TestSettings settings)
    {
        this.settings = settings;
    }

    private int testBufferSize(TCPSession sess)
    {
        if (settings.getRandomBufferSizes()) {
            return MetaEnv.rng()
                .nextInt(settings.getMaxRandomBufferSize()
                         - settings.getMinRandomBufferSize() + 1)
                + settings.getMinRandomBufferSize();
        } else {
            return sess.clientReadBufferSize();
        }
    }

    private Mode getLocalMode(boolean requestTime)
    {
        Mode[] modes = new Mode[10];
        int i=0;

        if (settings.isNormal()) {
            modes[i++] = Mode.NORMAL;
        }

        if (settings.isBuffered()) {
            modes[i++] = Mode.BUFFERED;
        }

        if (requestTime && settings.getRelease()) {
            modes[i++] = Mode.RELEASE;
        }

        i = MetaEnv.rng().nextInt(i);
        return modes[i];
    }

    public void handleTCPNewSessionRequest (TCPNewSessionRequestEvent event)
    {
        TCPNewSessionRequest sessReq = event.sessionRequest();

        if (!settings.isQuiet())
            logger.debug("New TCP Session Request " +
                         sessReq.clientAddr().getHostAddress() + ":" + sessReq.clientPort() + " -> " +
                         sessReq.serverAddr().getHostAddress() + ":" + sessReq.serverPort());

        Mode localMode = getLocalMode(true);

        if (Mode.RELEASE == localMode) {
            logger.debug("Releasing New TCP Session ");
            sessReq.release();
            return;
        }
    }

    public void handleTCPNewSession (TCPSessionEvent event)
    {
        TCPSession sess = event.session();
        Mode localMode = getLocalMode(false);
        // Pick session parameters at random to allow testing more codepaths.
        int crbs = testBufferSize(sess);
        int srbs = testBufferSize(sess);
        sess.clientReadBufferSize(crbs);
        sess.serverReadBufferSize(srbs);
        if (Mode.BUFFERED == localMode) {
            if (!settings.isQuiet())
                logger.debug(" Type: Line Buffered");
            sess.clientLineBuffering(true);
            sess.serverLineBuffering(true);
        }
        TestSessionState sss = new TestSessionState();
        sss.localMode = localMode;
        sess.attach(sss);
        if (!settings.isQuiet())
            logger.debug("New TCP Session " +
                         sess.clientAddr().getHostAddress() + ":" + sess.clientPort() + " -> " +
                         sess.serverAddr().getHostAddress() + ":" + sess.serverPort());
    }

    public void handleUDPNewSessionRequest (UDPNewSessionRequestEvent event)
    {
        UDPNewSessionRequest sessReq = event.sessionRequest();

        if (!settings.isQuiet())
            logger.debug("New UDP Session Request " +
                         sessReq.clientAddr().getHostAddress() + ":" + sessReq.clientPort() + " -> " +
                         sessReq.serverAddr().getHostAddress() + ":" + sessReq.serverPort());

        Mode localMode = getLocalMode(true);

        if (Mode.RELEASE == localMode) {
            logger.debug("Releasing New UDP Session ");
            sessReq.release();
            return;
        }
    }

    public void handleUDPNewSession (UDPSessionEvent event)
    {
        UDPSession sess = event.session();
        Mode localMode = getLocalMode(false);

        int maxPacketSize = 65536;
        sess.clientMaxPacketSize(maxPacketSize);
        sess.serverMaxPacketSize(maxPacketSize);

        TestSessionState sss = new TestSessionState();
        sss.localMode = localMode;
        sess.attach(sss);
        if (!settings.isQuiet())
            logger.debug("New UDP Session " +
                         sess.clientAddr().getHostAddress() + ":" + sess.clientPort() + " -> " +
                         sess.serverAddr().getHostAddress() + ":" + sess.serverPort());
    }

    public void handleUDPClientExpired(UDPSessionEvent event)
        throws MPipeException
    {
        UDPSession sess = event.session();
        if (!settings.isQuiet())
            logger.debug("UDP Client Expired " +
                         sess.clientAddr().getHostAddress() + ":" + sess.clientPort() + " -> " +
                         sess.serverAddr().getHostAddress() + ":" + sess.serverPort());
        super.handleUDPClientExpired(event);
    }

    public void handleUDPServerExpired(UDPSessionEvent event)
        throws MPipeException
    {
        UDPSession sess = event.session();
        if (!settings.isQuiet())
            logger.debug("UDP Server Expired " +
                         sess.clientAddr().getHostAddress() + ":" + sess.clientPort() + " -> " +
                         sess.serverAddr().getHostAddress() + ":" + sess.serverPort());
        super.handleUDPServerExpired(event);
    }

    public void handleTCPClientRST(TCPSessionEvent event)
        throws MPipeException
    {
        TCPSession sess = event.session();
        if (!settings.isQuiet())
            logger.warn("TCP Client RST " +
                        sess.clientAddr().getHostAddress() + ":" + sess.clientPort() + " -> " +
                        sess.serverAddr().getHostAddress() + ":" + sess.serverPort());
        super.handleTCPClientRST(event);
    }

    public void handleTCPServerRST(TCPSessionEvent event)
        throws MPipeException
    {
        TCPSession sess = event.session();
        if (!settings.isQuiet())
            logger.warn("TCP Server RST " +
                        sess.clientAddr().getHostAddress() + ":" + sess.clientPort() + " -> " +
                        sess.serverAddr().getHostAddress() + ":" + sess.serverPort());
        super.handleTCPServerRST(event);
    }

    public void handleTCPClientFIN(TCPSessionEvent event)
        throws MPipeException
    {
        TCPSession sess = event.session();
        if (!settings.isQuiet())
            logger.debug("TCP Client Input Shutdown " +
                         sess.clientAddr().getHostAddress() + ":" + sess.clientPort() + " -> " +
                         sess.serverAddr().getHostAddress() + ":" + sess.serverPort());
        super.handleTCPClientFIN(event);
        sess.scheduleTimer(HALF_OPEN_TIMEOUT);
    }

    public void handleTCPServerFIN(TCPSessionEvent event)
        throws MPipeException
    {
        TCPSession sess = event.session();
        if (!settings.isQuiet())
            logger.debug("TCP Server Input Shutdown " +
                         sess.clientAddr().getHostAddress() + ":" + sess.clientPort() + " -> " +
                         sess.serverAddr().getHostAddress() + ":" + sess.serverPort());
        super.handleTCPServerFIN(event);
        sess.scheduleTimer(HALF_OPEN_TIMEOUT);
    }

    public void handleTCPFinalized(TCPSessionEvent event)
        throws MPipeException
    {
        TCPSession sess = event.session();
        if (!settings.isQuiet())
            logger.debug("TCP Finalized " +
                         sess.clientAddr().getHostAddress() + ":" + sess.clientPort() + " -> " +
                         sess.serverAddr().getHostAddress() + ":" + sess.serverPort());
        super.handleTCPFinalized(event);
    }

    public void handleUDPFinalized(UDPSessionEvent event)
        throws MPipeException
    {
        UDPSession sess = event.session();
        if (!settings.isQuiet())
            logger.debug("UDP Finalized " +
                         sess.clientAddr().getHostAddress() + ":" + sess.clientPort() + " -> " +
                         sess.serverAddr().getHostAddress() + ":" + sess.serverPort());
        super.handleUDPFinalized(event);
    }

    public void handleTimer(IPSessionEvent event)
    {
        IPSession sess = event.ipsession();
        logger.debug("Timing out session in client state " +
                     SessionUtil.prettyState(sess.clientState()) + " server state " +
                     SessionUtil.prettyState(sess.serverState()) + " after " +
                     HALF_OPEN_TIMEOUT + "ms of inactivity");
        // XXXX
        // sess.close();
    }

    public IPDataResult handleTCPClientChunk(TCPChunkEvent event)
    {
        TCPSession sess = event.session();
        ByteBuffer buff = event.chunk();
        if (sess.clientState() == TCPSessionDesc.HALF_OPEN_OUTPUT) {
            // Reset the timer.
            sess.scheduleTimer(HALF_OPEN_TIMEOUT);
            if (!settings.isQuiet())
                logger.debug("Resetting half open timer");
        }
        if (!settings.isQuiet())
            logger.debug("Passing chunk size " + buff.remaining() + " bytes to Server");
        return IPDataResult.PASS_THROUGH;
        // if (sess.doubleBuffered()) {
        // copy it
        // return new IPDataResult(new ByteBuffer[] { copy_of_chunk });
    }

    public IPDataResult handleTCPServerChunk(TCPChunkEvent event)
    {
        TCPSession sess = event.session();
        ByteBuffer buff = event.chunk();
        if (sess.serverState() == TCPSessionDesc.HALF_OPEN_OUTPUT) {
            // Reset the timer.
            sess.scheduleTimer(HALF_OPEN_TIMEOUT);
            if (!settings.isQuiet())
                logger.debug("Resetting half open timer");
        }
        if (!settings.isQuiet())
            logger.debug("Passing chunk size " + buff.remaining() + " bytes to Client");
        return IPDataResult.PASS_THROUGH;
        // if (sess.doubleBuffered()) {
        // copy it
        // return new IPDataResult(new ByteBuffer[] { copy_of_chunk });
    }

    public void handleUDPClientPacket(UDPPacketEvent event)
        throws MPipeException
    {
        UDPSession sess = event.session();
        ByteBuffer packet = event.packet();
        if (!settings.isQuiet())
            logger.debug("Passing packet size " + packet.remaining() + " bytes to server");
        // if (sess.doubleBuffered()) {
        // copy it
        // return new IPDataResult(new ByteBuffer[] { copy_of_packet });

        // Send it through;
        super.handleUDPClientPacket(event);
    }

    public void handleUDPServerPacket(UDPPacketEvent event)
        throws MPipeException
    {
        UDPSession sess = event.session();
        ByteBuffer packet = event.packet();
        if (!settings.isQuiet())
            logger.debug("Passing packet size " + packet.remaining() + " bytes to client");
        // if (sess.doubleBuffered()) {
        // copy it
        // return new IPDataResult(new ByteBuffer[] { copy_of_packet });

        // Send it through;
        super.handleUDPServerPacket(event);
    }

    static class TestSessionState {

        public static final int NO_LEFTOVER = -1;

        // For double-buffered sessions, if we couldn't write the whole read
        // buffer to the write buffer then we left off at this position.  Set
        // to NO_LEFTOVER.
        int serverReadPosition = NO_LEFTOVER;

        int clientReadPosition = NO_LEFTOVER;

        Mode localMode;
    }
}

