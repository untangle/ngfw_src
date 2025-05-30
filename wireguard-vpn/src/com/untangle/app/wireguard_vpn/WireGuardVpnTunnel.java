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
 * Settings for the WireGuardVpn app.
 */
@SuppressWarnings("serial")
public class WireGuardVpnTunnel implements Serializable, JSONString
{
    private Integer id;
    private Boolean enabled = true;
    private String description = "";
    // Only required for dynamic endpoints
    private String publicKey = "";
    private String privateKey = "";
    private Boolean endpointDynamic = true;
    private InetAddress endpointAddress = null;
    private String endpointHostname = "";
    private Integer endpointPort = 51820;
    private InetAddress peerAddress;
    private String networks = "";
    private InetAddress pingAddress = null;
    private Integer pingInterval = 60;
    private Boolean pingConnectionEvents = true;
    private Boolean pingUnreachableEvents = false;
    // Only required for dynamic endpoints
    private Boolean assignDnsServer = false;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean newValue ) { this.enabled = newValue; }

    public Integer getId() { return id; }
    public void setId( Integer newValue ) { this.id = newValue; }

    public String getDescription() { return description; }
    public void setDescription( String newValue ) { this.description = newValue; }

    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey( String newValue ) { this.privateKey = newValue; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey( String newValue ) { this.publicKey = newValue; }

    public Boolean getEndpointDynamic() { return endpointDynamic; }
    public void setEndpointDynamic( Boolean newValue ) { this.endpointDynamic = newValue; }

    /* TODO: Take endpointAddress functions out after 16.3 */
    public InetAddress getEndpointAddress() { return endpointAddress; }
    public void setEndpointAddress( InetAddress newValue ) { this.endpointAddress = newValue; }

    public String getEndpointHostname() { return endpointHostname; }
    public void setEndpointHostname( String newValue ) { this.endpointHostname = newValue; }

    public Integer getEndpointPort() { return endpointPort; }
    public void setEndpointPort( Integer newValue ) { this.endpointPort = newValue; }

    public InetAddress getPeerAddress() { return peerAddress; }
    public void setPeerAddress( InetAddress newValue ) { this.peerAddress = newValue; }

    public String getNetworks() { return networks; }
    public void setNetworks( String newValue ) { this.networks = newValue; }

    public InetAddress getPingAddress() { return(pingAddress); }
    public void setPingAddress(InetAddress pingAddress) { this.pingAddress = pingAddress; }

    public int getPingInterval() { return(pingInterval); }
    public void setPingInterval(int pingInterval) { this.pingInterval = pingInterval; }

    public Boolean getPingConnectionEvents() { return pingConnectionEvents; }
    public void setPingConnectionEvents( Boolean newValue ) { this.pingConnectionEvents = newValue; }

    public Boolean getPingUnreachableEvents() { return pingUnreachableEvents; }
    public void setPingUnreachableEvents( Boolean newValue ) { this.pingUnreachableEvents = newValue; }

    public Boolean getAssignDnsServer() { return assignDnsServer; }
    public void setAssignDnsServer(Boolean assignDnsServer) { this.assignDnsServer = assignDnsServer; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
