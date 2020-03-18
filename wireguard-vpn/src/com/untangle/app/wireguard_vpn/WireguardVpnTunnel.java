/**
 * $Id$
 */

package com.untangle.app.wireguard_vpn;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;

import java.net.InetAddress;
import com.untangle.uvm.app.IPMaskedAddress;

/**
 * Settings for the WireguardVpn app.
 */
@SuppressWarnings("serial")
public class WireguardVpnTunnel implements Serializable, JSONString
{
    private Boolean enabled = true;
    private String description = "";
    private String publicKey = "";
    private Boolean endpointDynamic = true;
    private InetAddress endpointAddress; 
    private Integer endpointPort = 51820;
    private InetAddress peerAddress;
    private List<IPMaskedAddress> networks = null;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean newValue ) { this.enabled = newValue; }

    public String getDescription() { return description; }
    public void setDescription( String newValue ) { this.description = newValue; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey( String newValue ) { this.publicKey = newValue; }

    public Boolean getEndpointDynamic() { return endpointDynamic; }
    public void setEndpointDynamic( Boolean newValue ) { this.endpointDynamic = newValue; }

    public InetAddress getEndpointAddress() { return endpointAddress; }
    public void setEndpointAddress( InetAddress newValue ) { this.endpointAddress = newValue; }

    public Integer getEndpointPort() { return endpointPort; }
    public void setEndpointPort( Integer newValue ) { this.endpointPort = newValue; }

    public InetAddress getPeerAddress() { return peerAddress; }
    public void setPeerAddress( InetAddress newValue ) { this.peerAddress = newValue; }

    public List<IPMaskedAddress> getNetworks() { return networks; }
    public void setNetworks( List<IPMaskedAddress> newValue ) { this.networks = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
