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
    private String protocol = "udp"; /* "tcp" or "udp" */
    private int port = 1194;
    private String cipher = "AES-128-CBC";
    
    private String siteName = "untangle";
    private IPMaskedAddress addressSpace;

    private boolean serverEnabled = true;
    
    /**
     * List of addresses visible to those connecting to the VPN
     */
    private List<OpenVpnExport> exports = new LinkedList<OpenVpnExport>();

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
    private List<OpenVpnRemoteServer> remoteServers = new LinkedList<OpenVpnRemoteServer>();

    public OpenVpnSettings() { }

    public boolean getServerEnabled() { return this.serverEnabled; }
    public void setServerEnabled( boolean newValue ) { this.serverEnabled = newValue; }

    public String getProtocol() { return this.protocol; }
    public void setProtocol( String newValue ) { this.protocol = newValue; }

    public int getPort() { return this.port; }
    public void setPort( int newValue ) { this.port = newValue; }

    public String getCipher() { return this.cipher; }
    public void setCipher( String newValue ) { this.cipher = newValue; }
    
    public String getSiteName() { return this.siteName; }
    public void setSiteName( String newValue ) { this.siteName = newValue; }

    public IPMaskedAddress getAddressSpace() { return this.addressSpace; }
    public void setAddressSpace( IPMaskedAddress newValue ) { this.addressSpace = newValue; }

    public List<OpenVpnExport> getExports() { return this.exports; }
    public void setExports( List<OpenVpnExport> newValue ) { this.exports = newValue; }

    public List<OpenVpnGroup> getGroups() { return this.groups; }
    public void setGroups( List<OpenVpnGroup> newValue ) { this.groups = newValue; }
    
    public List<OpenVpnRemoteClient> getRemoteClients() { return this.remoteClients; }
    public void setRemoteClients( List<OpenVpnRemoteClient> newValue ) { this.remoteClients = newValue; }

    public List<OpenVpnRemoteServer> getRemoteServers() { return this.remoteServers; }
    public void setRemoteServers( List<OpenVpnRemoteServer> newValue ) { this.remoteServers = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
