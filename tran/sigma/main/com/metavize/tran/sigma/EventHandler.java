/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.sigma;

import java.nio.*;
import java.util.ArrayList;
import java.util.regex.Pattern;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tapi.event.*;
import com.metavize.mvvm.tran.Transform;
import org.apache.log4j.Logger;

public class EventHandler extends AbstractEventHandler
{
    private SigmaSettings settings = null;
    private final Logger logger = Logger.getLogger(EventHandler.class);
    private final Logger eventLogger = MvvmContextFactory.context().eventLogger();

    private class SessionInfo {
        public int myState;
    }

    public EventHandler() {}

    public void setSettings(SigmaSettings settings)
    {
        this.settings = settings;
    }

    public SigmaSettings getSettings()
    {
        return this.settings;
    }

    public void handleTCPNewSession (TCPSessionEvent event)
    {
        TCPSession sess = event.session();
        SessionInfo sessInfo = new SessionInfo();
        sessInfo.myState = 1;
        sess.attach(sessInfo);
    }

    public void handleUDPNewSession (UDPSessionEvent event)
    {
        UDPSession sess = event.session();

        SessionInfo sessInfo = new SessionInfo();
        sessInfo.myState = 1;
        sess.attach(sessInfo);
    }

    public IPDataResult handleTCPClientChunk (TCPChunkEvent e)
    {
        return IPDataResult.PASS_THROUGH;
    }

    public IPDataResult handleTCPServerChunk (TCPChunkEvent e)
    {
        return IPDataResult.PASS_THROUGH;
    }

    public void handleUDPClientPacket (UDPPacketEvent e)
	   throws MPipeException
    {
        UDPSession sess = e.session();
        sess.sendClientPacket(e.packet(), e.header());
    }

    public void handleUDPServerPacket (UDPPacketEvent e)
	   throws MPipeException
    {
        UDPSession sess = e.session();
        sess.sendClientPacket(e.packet(), e.header());
    }

}
