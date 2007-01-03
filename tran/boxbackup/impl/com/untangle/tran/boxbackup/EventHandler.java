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

package com.untangle.tran.boxbackup;

import java.nio.*;

import com.untangle.mvvm.tapi.*;
import com.untangle.mvvm.tapi.event.*;
import com.untangle.mvvm.tran.Transform;
import org.apache.log4j.Logger;

public class EventHandler extends AbstractEventHandler
{
    private BoxBackupSettings settings = null;
    private final Logger logger = Logger.getLogger(EventHandler.class);

    private class SessionInfo {
        public int myState;
    }

    public EventHandler(Transform transform)
    {
        super(transform);
    }

    public void setSettings(BoxBackupSettings settings)
    {
        this.settings = settings;
    }

    public BoxBackupSettings getSettings()
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
