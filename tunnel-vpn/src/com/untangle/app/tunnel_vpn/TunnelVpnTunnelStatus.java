/**
 * $Id$
 */

package com.untangle.app.tunnel_vpn;

import java.net.InetAddress;
import java.util.Date;

public class TunnelVpnTunnelStatus
{
    Integer clientId;
    InetAddress serverAddress;
    InetAddress localAddress;
    String stateInfo = "UNKNOWN";
    String stateLast = "UNKNOWN";
    long connectStamp = 0;
    long xmitTotal = 0;
    long xmitLast = 0;
    long recvTotal = 0;
    long recvLast = 0;

    public String toString()
    {
        String string = new String();
        string += (" STAMP:" + Long.toString(connectStamp));
        string += (" SERVER:" + serverAddress.getHostAddress().toString());
        string += (" LOCAL:" + localAddress.getHostAddress().toString());
        string += (" INFO:" + stateInfo);
        string += (" XMIT:" + Long.toString(xmitTotal));
        string += (" RECV:" + Long.toString(recvTotal));
        return (string);
    }
}
