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

import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.argon.SessionEndpoints;
import java.net.InetAddress;

public class UDPSessionDescImpl extends IPSessionDescImpl implements UDPSessionDesc {

    public UDPSessionDescImpl(int id, SessionStats stats,
                              byte clientState, byte serverState, 
                              byte clientIntf, byte serverIntf, 
                              InetAddress clientAddr, InetAddress serverAddr,
                              int clientPort, int serverPort, boolean isInbound)
    {
        super(id, SessionEndpoints.PROTO_UDP, stats, clientState, serverState,
              clientIntf, serverIntf, clientAddr, serverAddr, clientPort, serverPort, isInbound);
    }

    public boolean isPing()
    {
        return (clientPort == 0 && serverPort == 0);
    }
    
    /* XXX Not sure what this is XXX This should be implemented for sure */
    public int icmpId()
    {
        return 0;
    }
}
