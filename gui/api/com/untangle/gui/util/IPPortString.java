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

package com.untangle.gui.util;

import java.net.InetAddress;

import com.untangle.uvm.node.IPaddr;

public class IPPortString implements Comparable<IPPortString> {

    private IPaddr ipAddr;
    private int port;

    public IPPortString(){}

    public IPPortString( IPaddr ipAddr, int port ){
        this.ipAddr = ipAddr;
        this.port = port;
    }

    public IPPortString( InetAddress inetAddress, int port ){
        this.ipAddr = new IPaddr(inetAddress);
        this.port = port;
    }

    public boolean equals(Object obj){
        if( !(obj instanceof IPPortString) )
            return false;
        else
            return 0 == compareTo( (IPPortString) obj );
    }

    public int compareTo(IPPortString ipPortString){
        if( (ipAddr == null) && (ipPortString.ipAddr == null) )
            return 0;
        else if( (ipAddr != null) && (ipPortString.ipAddr == null) )
            return 1;
        else if( (ipAddr == null) && (ipPortString.ipAddr != null) )
            return -1;
        else{
            int ipAddrComparison = ipAddr.compareTo(ipPortString.ipAddr);
            if( ipAddrComparison == -1 )
                return -1;
            else if( ipAddrComparison == 1 )
                return 1;
            else{
                if( port < ipPortString.port )
                    return -1;
                else if( port > ipPortString.port )
                    return 1;
                else
                    return 0;
            }
        }
    }

    public String toString(){
        if( ipAddr == null )
            return "";
        else
            return ipAddr.toString() + ":" + Integer.toString(port);
    }
}
