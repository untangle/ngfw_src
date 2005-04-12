/*
 * Copyright (c) 2003, 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.tapi.client;

import com.metavize.mvvm.tapi.IPSessionDesc;
import com.metavize.mvvm.tapi.SessionStats;
import java.net.InetAddress;

class IPSessionDescImpl extends SessionDescImpl implements IPSessionDesc {

    protected byte clientState;
    protected byte serverState;

    protected short protocol;

    protected byte clientIntf;
    protected byte serverIntf;

    protected InetAddress clientAddr;
    protected InetAddress serverAddr;

    protected int clientPort;
    protected int serverPort;

    protected IPSessionDescImpl(int id, short protocol, SessionStats stats,
                                byte clientState, byte serverState,
                                byte clientIntf, byte serverIntf,
                                InetAddress clientAddr, InetAddress serverAddr,
                                int clientPort, int serverPort)
    {
        super(id, stats);
        this.protocol = protocol;
        this.clientState = clientState;
        this.serverState = serverState;
        this.clientIntf = clientIntf;
        this.serverIntf = serverIntf;
        this.clientAddr = clientAddr;
        this.serverAddr = serverAddr;
        this.clientPort = clientPort;
        this.serverPort = serverPort;
    }

    public short protocol()
    {
        return protocol;
    }
    
    public byte clientIntf()
    {
        return clientIntf;
    }

    public byte serverIntf()
    {
        return serverIntf;
    }

    // XX Only works for max two interfaces.
    public byte direction()
    {
        if (clientIntf == 0)
            return INBOUND;
        else
            return OUTBOUND;
    }

    public byte clientState()
    {
        return clientState;
    }

    public byte serverState()
    {
        return serverState;
    }

    public InetAddress clientAddr()
    {
        return clientAddr;
    }

    public InetAddress serverAddr()
    {
        return serverAddr;
    }

    public int clientPort()
    {
        return clientPort;
    }

    public int serverPort()
    {
        return serverPort;
    }
}
