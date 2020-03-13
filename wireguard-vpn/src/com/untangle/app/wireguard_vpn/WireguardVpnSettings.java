/**
 * $Id$
 */

package com.untangle.app.wireguard_vpn;

import java.io.Serializable;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.app.IPMaskedAddress;

/**
 * Settings for the WireguardVpn app.
 */
@SuppressWarnings("serial")
public class WireguardVpnSettings implements Serializable, JSONString
{
    private Integer version = 1;

    private Integer keepaliveInterval = 25;
    private Integer listenPort = 51820;
    private Integer mtu = 1420;
    private IPMaskedAddress addressPool;
    private String privateKey = "";
    private String publicKey = "";

    private List<WireguardVpnTunnel> tunnels;

    public Integer getVersion() { return version; }
    public void setVersion( Integer newValue ) { this.version = newValue; }

    public Integer getKeepaliveInterval() { return keepaliveInterval; }
    public void setKeepaliveInterval( Integer newValue ) { this.keepaliveInterval = newValue; }

    public Integer getListenPort() { return listenPort; }
    public void setListenPort( Integer newValue ) { this.listenPort = newValue; }

    public Integer getMtu() { return mtu; }
    public void setMtu( Integer newValue ) { this.mtu = newValue; }

    public IPMaskedAddress getAddressPool() { return addressPool; }
    public void setAddressPool( IPMaskedAddress newValue ) { this.addressPool = newValue; }

    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey( String newValue ) { this.privateKey = newValue; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey( String newValue ) { this.publicKey = newValue; }

    public List<WireguardVpnTunnel> getTunnels() { return tunnels; }
    public void setTunnels( List<WireguardVpnTunnel> newValue ) { this.tunnels = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
