/**
 * $Id$
 */

package com.untangle.app.tunnel_vpn;

import java.net.InetAddress;
import java.io.Serializable;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * Class for managing the status of a VPN tunnel
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class TunnelVpnTunnelStatus implements JSONString, Serializable
{
    private InetAddress serverAddress;
    private InetAddress localAddress;
    private String tunnelName;
    private String stateInfo = STATE_DISCONNECTED;
    private String stateLast = STATE_DISCONNECTED;
    private long cycleCount = 0;
    private long connectStamp = 0;
    private long xmitTotal = 0;
    private long xmitLast = 0;
    private long recvTotal = 0;
    private long recvLast = 0;
    private int tunnelId = 0;

    protected long restartCount = 0;
    protected long restartStamp = 0;

    public static final String STATE_DISCONNECTED = "DISCONNECTED";
    public static final String STATE_CONNECTED = "CONNECTED";

    public TunnelVpnTunnelStatus(int tunnelId)
    {
        this.tunnelId = tunnelId;
    }

// THIS IS FOR ECLIPSE - @formatter:off


        public InetAddress getServerAddress() { return(serverAddress); }
        public void setServerAddress(InetAddress argValue) { this.serverAddress = argValue; }

        public InetAddress getLocalAddress() { return(localAddress); }
        public void setLocalAddress(InetAddress argValue) { this.localAddress = argValue; }

        public String getTunnelName() { return(tunnelName); }
        public void setTunnelName(String argValue) { this.tunnelName = argValue; }

        public String getStateInfo() { return(stateInfo); }
        public void setStateInfo(String argValue) { this.stateInfo = argValue; }

        public String getStateLast() { return(stateLast); }
        public void setStateLast(String argValue) { this.stateLast = argValue; }

        public long getCycleCount() { return(cycleCount); }
        public void setCycleCount(long argValue) { this.cycleCount = argValue; }

        public long getConnectStamp() { return(connectStamp); }
        public void setConnectStamp(long argValue) { this.connectStamp = argValue; }

        public long getXmitTotal() { return(xmitTotal); }
        public void setXmitTotal(long argValue) { this.xmitTotal = argValue; }

        public long getXmitLast() { return(xmitLast); }
        public void setXmitLast(long argValue) { this.xmitLast = argValue; }

        public long getRecvTotal() { return(recvTotal); }
        public void setRecvTotal(long argValue) { this.recvTotal = argValue; }

        public long getRecvLast() { return(recvLast); }
        public void setRecvLast(long argValue) { this.recvLast = argValue; }

        public int getTunnelId() { return(tunnelId); }
        public void setTunnelId(int argValue) { this.tunnelId = argValue; }

        public long getElapsedTime()
        {
            if (!stateInfo.equals(STATE_CONNECTED)) return(0);
            return (System.currentTimeMillis() - connectStamp * 1000);
        }

        public void clearTunnelStatus()
        {
            stateInfo = STATE_DISCONNECTED;
            connectStamp = 0;
            xmitTotal = xmitLast = 0;
            recvTotal = recvLast = 0;
            restartCount = restartStamp = 0;
        }

// THIS IS FOR ECLIPSE - @formatter:on

    public String toString()
    {
        String string = new String();
        string += (" TUNNEL:" + tunnelName + "(" + Integer.toString(getTunnelId()) + ")");
        string += (" STAMP:" + Long.toString(connectStamp));
        string += (" SERVER:" + (serverAddress == null ? "x.x.x.x" : serverAddress.getHostAddress().toString()));
        string += (" LOCAL:" + (localAddress == null ? "x.x.x.x" : localAddress.getHostAddress().toString()));
        string += (" INFO:" + stateInfo + " LAST:" + stateLast);
        string += (" RECV:" + Long.toString(recvTotal) + " LAST:" + Long.toString(recvLast));
        string += (" XMIT:" + Long.toString(xmitTotal) + " LAST:" + Long.toString(xmitLast));
        return (string);
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
