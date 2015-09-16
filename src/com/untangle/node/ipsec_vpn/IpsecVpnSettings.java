/**
 * $Id: IpsecVpnSettings.java 40630 2015-07-06 18:13:13Z mahotz $
 */

package com.untangle.node.ipsec_vpn;

import java.util.LinkedList;
import org.json.JSONString;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class IpsecVpnSettings implements java.io.Serializable, JSONString
{
    public static enum AuthenticationType
    {
        LOCAL_DIRECTORY, RADIUS_SERVER
    };

    private LinkedList<IpsecVpnTunnel> tunnelList = new LinkedList<IpsecVpnTunnel>();
    private boolean bypassflag = false;
    private boolean debugflag = false;
    private boolean vpnflag = false;
    private LinkedList<VirtualListen> virtualListenList = new LinkedList<VirtualListen>();
    private AuthenticationType authenticationType = AuthenticationType.LOCAL_DIRECTORY;
    private String virtualAddressPool = "198.18.0.0/16";
    private String virtualSecret = "Please_Change_Me";
    private String virtualDnsOne = "";
    private String virtualDnsTwo = "";
    private String virtualXauthPool = "198.19.0.0/16";
    private String charonDebug = "";

    public IpsecVpnSettings()
    {
    }

    // THIS IS FOR ECLIPSE - @formatter:off

    public boolean getBypassflag() { return (bypassflag); }
    public void setBypassflag(boolean bypassflag) { this.bypassflag = bypassflag; }

    public boolean getDebugflag() { return (debugflag); }
    public void setDebugflag(boolean debugflag) { this.debugflag = debugflag; }

    public boolean getVpnflag() { return (vpnflag); }
    public void setVpnflag(boolean vpnflag) { this.vpnflag = vpnflag; }

    public LinkedList<IpsecVpnTunnel> getTunnels() { return (tunnelList); }
    public void setTunnels(LinkedList<IpsecVpnTunnel> tunnelList) { this.tunnelList = tunnelList; }

    public LinkedList<VirtualListen> getVirtualListenList() { return (virtualListenList); }
    public void setVirtualListenList(LinkedList<VirtualListen> virtualListenList) { this.virtualListenList = virtualListenList; }

    public AuthenticationType getAuthenticationType() { return this.authenticationType; }
    public void setAuthenticationType(AuthenticationType newValue) { this.authenticationType = newValue; }

    public String getVirtualAddressPool() { return (virtualAddressPool); }
    public void setVirtualAddressPool(String virtualAddressPool) { this.virtualAddressPool = virtualAddressPool; }

    public String getVirtualSecret() { return (virtualSecret); }
    public void setVirtualSecret(String virtualSecret) { this.virtualSecret = virtualSecret; }

    public String getVirtualDnsOne() { return(virtualDnsOne); }
    public void setVirtualDnsOne(String virtualDnsOne) { this.virtualDnsOne = virtualDnsOne; }

    public String getVirtualDnsTwo() { return(virtualDnsTwo); }
    public void setVirtualDnsTwo(String virtualDnsTwo) { this.virtualDnsTwo = virtualDnsTwo; }

    public String getVirtualXauthPool() { return (virtualXauthPool); }
    public void setVirtualXauthPool(String virtualXauthPool) { this.virtualXauthPool = virtualXauthPool; }

    public String getCharonDebug() { return (charonDebug); }
    public void setCharonDebug(String charonDebug) { this.charonDebug = charonDebug; }

// THIS IS FOR ECLIPSE - @formatter:off

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
