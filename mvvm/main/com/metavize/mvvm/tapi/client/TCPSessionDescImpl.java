/*
 * Copyright (c) 2003, 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: TCPSessionDescImpl.java,v 1.5 2005/01/05 01:22:34 jdi Exp $
 */

package com.metavize.mvvm.tapi.client;

import com.metavize.mvvm.tapi.TCPSessionDesc;
import com.metavize.mvvm.tapi.SessionStats;
import java.net.InetAddress;

public class TCPSessionDescImpl extends IPSessionDescImpl implements TCPSessionDesc {

    public TCPSessionDescImpl(int id, SessionStats stats,
                              byte clientState, byte serverState, 
                              byte clientIntf, byte serverIntf, 
                              InetAddress clientAddr, InetAddress serverAddr,
                              int clientPort, int serverPort)
    {
        super(id, stats, clientState, serverState,
              clientIntf, serverIntf, clientAddr, serverAddr, clientPort, serverPort);
    }
}
