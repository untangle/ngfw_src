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

package com.metavize.mvvm.tapi.client;

import com.metavize.mvvm.tapi.TCPSessionDesc;
import com.metavize.mvvm.tapi.SessionStats;
import com.metavize.mvvm.argon.SessionEndpoints;
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
