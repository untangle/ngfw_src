/*
 * Copyright (c) 2003, 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: UDPSessionDescImpl.java,v 1.8 2005/01/06 02:39:41 jdi Exp $
 */

package com.metavize.mvvm.tapi.client;

import com.metavize.mvvm.tapi.*;
import java.net.InetAddress;

public class UDPSessionDescImpl extends IPSessionDescImpl implements UDPSessionDesc {

    public UDPSessionDescImpl(int id, SessionStats stats,
                              byte clientState, byte serverState, 
                              byte clientIntf, byte serverIntf, 
                              InetAddress clientAddr, InetAddress serverAddr,
                              int clientPort, int serverPort)
    {
        super(id, stats, clientState, serverState,
              clientIntf, serverIntf, clientAddr, serverAddr, clientPort, serverPort);
    }
}
