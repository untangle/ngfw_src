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
    private String virtualNetworkPool = ""; // used for GRE
    private String virtualAddressPool = ""; // used for L2TP
    private String virtualXauthPool = ""; // used for XAUTH
    private String virtualSecret = "Please_Change_Me";
    private String virtualDnsOne = "";
    private String virtualDnsTwo = "";
    private String charonDebug = "";
    private String uniqueIds = "yes";
    private boolean phase1Manual = false;
    private String phase1Cipher = "3des";
    private String phase1Hash = "md5";
    private String phase1Group = "modp2048";
    private String phase1Lifetime = "28800";
    private boolean phase2Manual = false;
    private String phase2Cipher = "3des";
    private String phase2Hash = "md5";
    private String phase2Group = "modp2048";
    private String phase2Lifetime = "3600";
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

    public boolean getPhase1Manual() { return (phase1Manual); }
    public void setPhase1Manual(boolean phase1Manual) { this.phase1Manual = phase1Manual; }

    public String getPhase1Cipher() { return (phase1Cipher); }
    public void setPhase1Cipher(String phase1Cipher) { this.phase1Cipher = phase1Cipher; }

    public String getPhase1Hash() { return (phase1Hash); }
    public void setPhase1Hash(String phase1Hash) { this.phase1Hash = phase1Hash; }

    public String getPhase1Group() { return (phase1Group); }
    public void setPhase1Group(String phase1Group) { this.phase1Group = phase1Group; }

    public String getPhase1Lifetime() { return (phase1Lifetime); }
    public void setPhase1Lifetime(String phase1Lifetime) { this.phase1Lifetime = phase1Lifetime; }

    public boolean getPhase2Manual() { return (phase2Manual); }
    public void setPhase2Manual(boolean phase2Manual) { this.phase2Manual = phase2Manual; }

    public String getPhase2Cipher() { return (phase2Cipher); }
    public void setPhase2Cipher(String phase2Cipher) { this.phase2Cipher = phase2Cipher; }

    public String getPhase2Hash() { return (phase2Hash); }
    public void setPhase2Hash(String phase2Hash) { this.phase2Hash = phase2Hash; }

    public String getPhase2Group() { return (phase2Group); }
    public void setPhase2Group(String phase2Group) { this.phase2Group = phase2Group; }

    public String getPhase2Lifetime() { return (phase2Lifetime); }
    public void setPhase2Lifetime(String phase2Lifetime) { this.phase2Lifetime = phase2Lifetime; }

// THIS IS FOR ECLIPSE - @formatter:on

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
