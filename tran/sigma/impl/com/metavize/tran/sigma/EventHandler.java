/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.sigma;

import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tapi.event.*;
import com.metavize.mvvm.tran.Transform;
import org.apache.log4j.Logger;

public class EventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());

    private class SessionInfo {
        public int myState;
    }

    public EventHandler(Transform transform)
    {
        super(transform);
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
