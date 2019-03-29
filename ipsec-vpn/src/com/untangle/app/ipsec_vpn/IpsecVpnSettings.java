/**
 * $Id: IpsecVpnSettings.java 40630 2015-07-06 18:13:13Z mahotz $
 */

package com.untangle.app.ipsec_vpn;

import java.util.LinkedList;
import org.json.JSONString;
import org.json.JSONObject;

/**
 * This class represents all of the settings for the IPsec application.
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class IpsecVpnSettings implements java.io.Serializable, JSONString
{
    public static enum AuthenticationType
    {
        LOCAL_DIRECTORY, RADIUS_SERVER
    };

    private LinkedList<IpsecVpnTunnel> tunnelList = new LinkedList<>();
    private LinkedList<IpsecVpnNetwork> networkList = new LinkedList<>();
    private boolean bypassflag = false;
    private boolean debugflag = false;
    private boolean vpnflag = false;
    private boolean neverWriteConfig = false;
    private boolean allowConcurrentLogins = true;
    private LinkedList<VirtualListen> virtualListenList = new LinkedList<>();
    private AuthenticationType authenticationType = AuthenticationType.LOCAL_DIRECTORY;
    private String virtualNetworkPool = "198.51.100.0/24"; // used for GRE
    private String virtualAddressPool = "198.18.0.0/16"; // used for L2TP
    private String virtualXauthPool = "198.19.0.0/16"; // used for XAUTH
    private String virtualSecret = "Please_Change_Me";
    private String virtualDnsOne = "";
    private String virtualDnsTwo = "";
    private String charonDebug = "";
    private String uniqueIds = "yes";
    private String phase1DefaultLifetime = "8h";
    private String phase2DefaultLifetime = "1h";

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

    public boolean getNeverWriteConfig() { return(neverWriteConfig); }
    public void setNeverWriteConfig(boolean neverWriteConfig) { this.neverWriteConfig = neverWriteConfig; }

    public boolean getAllowConcurrentLogins() { return(allowConcurrentLogins); }
    public void setAllowConcurrentLogins(boolean allowConcurrentLogins) { this.allowConcurrentLogins = allowConcurrentLogins; }

    public LinkedList<IpsecVpnTunnel> getTunnels() { return (tunnelList); }
    public void setTunnels(LinkedList<IpsecVpnTunnel> tunnelList) { this.tunnelList = tunnelList; }

    public LinkedList<IpsecVpnNetwork> getNetworks() { return (networkList); }
    public void setNetworks(LinkedList<IpsecVpnNetwork> networkList) { this.networkList = networkList; }

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
    
    public String getVirtualNetworkPool() { return (virtualNetworkPool); }
    public void setVirtualNetworkPool(String virtualNetworkPool) { this.virtualNetworkPool = virtualNetworkPool; }

    public String getCharonDebug() { return (charonDebug); }
    public void setCharonDebug(String charonDebug) { this.charonDebug = charonDebug; }

    public String getUniqueIds() { return(uniqueIds); }
    public void setUniqueIds(String uniqueIds) { this.uniqueIds = uniqueIds; }

    public String getPhase1DefaultLifetime() { return(phase1DefaultLifetime); }
    public void setPhase1DefaultLifetime(String argString) { this.phase1DefaultLifetime = argString; }

    public String getPhase2DefaultLifetime() { return(phase2DefaultLifetime); }
    public void setPhase2DefaultLifetime(String argString) { this.phase2DefaultLifetime = argString; }

// THIS IS FOR ECLIPSE - @formatter:on

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
