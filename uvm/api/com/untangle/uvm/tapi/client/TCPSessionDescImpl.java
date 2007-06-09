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

import com.untangle.uvm.node.SessionEndpoints;

import com.untangle.uvm.tapi.SessionStats;
import com.untangle.uvm.tapi.TCPSessionDesc;
import java.net.InetAddress;

public class TCPSessionDescImpl extends IPSessionDescImpl implements TCPSessionDesc {

    public TCPSessionDescImpl(int id, SessionStats stats,
                              byte clientState, byte serverState, 
                              byte clientIntf, byte serverIntf, 
                              InetAddress clientAddr, InetAddress serverAddr,
                              int clientPort, int serverPort, boolean isInbound)
    {
        super(id, SessionEndpoints.PROTO_TCP, stats, clientState, serverState,
              clientIntf, serverIntf, clientAddr, serverAddr, clientPort, serverPort, isInbound);
    }
}
