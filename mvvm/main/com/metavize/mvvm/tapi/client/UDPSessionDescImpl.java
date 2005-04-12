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

import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.argon.SessionEndpoints;
import java.net.InetAddress;

public class UDPSessionDescImpl extends IPSessionDescImpl implements UDPSessionDesc {

    public UDPSessionDescImpl(int id, SessionStats stats,
                              byte clientState, byte serverState, 
                              byte clientIntf, byte serverIntf, 
                              InetAddress clientAddr, InetAddress serverAddr,
                              int clientPort, int serverPort)
    {
        super(id, SessionEndpoints.PROTO_UDP, stats, clientState, serverState,
              clientIntf, serverIntf, clientAddr, serverAddr, clientPort, serverPort);
    }
}
