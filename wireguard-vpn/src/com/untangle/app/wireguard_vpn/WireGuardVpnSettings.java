/**
 * $Id$
 */

package com.untangle.app.wireguard_vpn;

import java.io.Serializable;
import java.util.List;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.app.IPMaskedAddress;

/**
 * Settings for the WireGuardVpn app.
 */
@SuppressWarnings("serial")
public class WireGuardVpnSettings implements Serializable, JSONString
{
    private Integer version = 6;

    private Integer keepaliveInterval = 25;
    private Integer listenPort = 51820;
    private Integer mtu = 1500;
    private boolean mapTunnelDescUser = false;
    private IPMaskedAddress addressPool;
    private String privateKey = "";
    private String publicKey = "";
    private InetAddress dnsServer;
    private String dnsSearchDomain = "";
    private List<WireGuardVpnNetwork> networks;
    private List<WireGuardVpnNetworkProfile> networkProfiles;
    private boolean autoAddressAssignment = true;

    private List<WireGuardVpnTunnel> tunnels;

    public Integer getVersion() { return version; }
    public void setVersion( Integer newValue ) { this.version = newValue; }

    public Integer getKeepaliveInterval() { return keepaliveInterval; }
    public void setKeepaliveInterval( Integer newValue ) { this.keepaliveInterval = newValue; }

    public Integer getListenPort() { return listenPort; }
    public void setListenPort( Integer newValue ) { this.listenPort = newValue; }

    public Integer getMtu() { return mtu; }
    public void setMtu( Integer newValue ) { this.mtu = newValue; }

    public boolean isMapTunnelDescUser() { return mapTunnelDescUser; }
    public void setMapTunnelDescUser(boolean mapTunnelDescUser) { this.mapTunnelDescUser = mapTunnelDescUser; }

    public IPMaskedAddress getAddressPool() { return addressPool; }
    public void setAddressPool( IPMaskedAddress newValue ) { this.addressPool = newValue; }

    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey( String newValue ) { this.privateKey = newValue; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey( String newValue ) { this.publicKey = newValue; }

    public InetAddress getDnsServer() { return dnsServer; }
    public void setDnsServer( InetAddress newValue ) { this.dnsServer = newValue; }

    public String getDnsSearchDomain() { return dnsSearchDomain; }
    public void setDnsSearchDomain(String dnsSearchDomain) { this.dnsSearchDomain = dnsSearchDomain; }

    public List<WireGuardVpnNetwork> getNetworks() { return networks; }
    public void setNetworks( List<WireGuardVpnNetwork> newValue ) { this.networks = newValue; }

    public List<WireGuardVpnNetworkProfile> getNetworkProfiles() { return networkProfiles; }
    public void setNetworkProfiles(List<WireGuardVpnNetworkProfile> networkProfiles) { this.networkProfiles = networkProfiles; }

    public boolean getAutoAddressAssignment() { return autoAddressAssignment; }
    public void setAutoAddressAssignment( boolean newValue ) { this.autoAddressAssignment = newValue; }

    public List<WireGuardVpnTunnel> getTunnels() { return tunnels; }
    public void setTunnels( List<WireGuardVpnTunnel> newValue ) { this.tunnels = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
