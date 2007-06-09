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

package com.untangle.uvm.tapi.client;

import com.untangle.uvm.tapi.IPSessionDesc;
import com.untangle.uvm.tapi.SessionStats;
import java.net.InetAddress;

class IPSessionDescImpl extends SessionDescImpl implements IPSessionDesc {

    protected byte clientState;
    protected byte serverState;

    protected short protocol;

    protected byte clientIntf;
    protected byte serverIntf;

    protected boolean isInbound;

    protected InetAddress clientAddr;
    protected InetAddress serverAddr;

    protected int clientPort;
    protected int serverPort;

    protected IPSessionDescImpl(int id, short protocol, SessionStats stats,
                                byte clientState, byte serverState,
                                byte clientIntf, byte serverIntf,
                                InetAddress clientAddr, InetAddress serverAddr,
                                int clientPort, int serverPort, boolean isInbound)
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
        this.isInbound = isInbound;
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

    public boolean isInbound()
    {
        return isInbound;
    }

    public boolean isOutbound()
    {
        return !isInbound;
    }
}
