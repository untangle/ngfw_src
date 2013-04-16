/**
 * $Id: OpenVpnSettings.java,v 1.00 2013/04/15 10:22:43 dmorris Exp $
 */
package com.untangle.node.openvpn;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.node.IPMaskedAddress;

/**
 * Settings for the open vpn node.
 */
@SuppressWarnings("serial")
public class OpenVpnSettings implements java.io.Serializable, JSONString
{
    private int port = 1194;
    private String siteName = "untangle";
    private InetAddress localAddress;
    
    /**
     * List of addresses visible to those connecting to the VPN
     */
    private List<IPMaskedAddress> exports = new LinkedList<IPMaskedAddress>();

    /**
     * List of the various group of remote clients
     */
    private List<OpenVpnGroup> groups = new LinkedList<OpenVpnGroup>();

    /**
     * List of all the remote clients
     */
    private List<OpenVpnRemoteClient> remoteClients = new LinkedList<OpenVpnRemoteClient>();

    /**
     * List of all the remote servers
     */
    private List<OpenVpnServer> remoteServers = new LinkedList<OpenVpnServer>();

    public OpenVpnSettings() { }

    public int getPort() { return this.port; }
    public void setPort( int newValue ) { this.port = newValue; }

    public String getSiteName() { return this.siteName; }
    public void setSiteName( String newValue ) { this.siteName = newValue; }

    public InetAddress getLocalAddress() { return this.localAddress; }
    public void setLocalAddress( InetAddress newValue ) { this.localAddress = newValue; }
    
    public List<IPMaskedAddress> getExports() { return this.exports; }
    public void setExports( List<IPMaskedAddress> newValue ) { this.exports = newValue; }

    public List<OpenVpnGroup> getGroups() { return this.groups; }
    public void setGroups( List<OpenVpnGroup> newValue ) { this.groups = newValue; }
    
    public List<OpenVpnRemoteClient> getRemoteClients() { return this.remoteClients; }
    public void setRemoteClients( List<OpenVpnRemoteClient> newValue ) { this.remoteClients = newValue; }

    public List<OpenVpnServer> getRemoteServers() { return this.remoteServers; }
    public void setRemoteServers( List<OpenVpnServer> newValue ) { this.remoteServers = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
