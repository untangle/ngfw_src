/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.tran.test;

import java.io.*;
import java.nio.*;

import com.untangle.mvvm.tapi.*;
import com.untangle.mvvm.tapi.event.*;
import com.untangle.mvvm.tran.Transform;
import com.untangle.mvvm.util.MetaEnv;
import com.untangle.mvvm.util.SessionUtil;
import org.apache.log4j.Logger;

public class EventHandler extends AbstractEventHandler
{
    private static final long HALF_OPEN_TIMEOUT = 10000; // 10 seconds

    private final Logger logger = Logger.getLogger(getClass());

    private TestSettings settings;

    public EventHandler(Transform transform, TestSettings settings)
    {
        super(transform);

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

