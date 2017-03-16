/**
 * $Id$
 */
package com.untangle.app.openvpn;

import java.net.InetAddress;

import com.untangle.uvm.app.IPMaskedAddress;

@SuppressWarnings("serial")
public class OpenVpnRemoteClient implements java.io.Serializable
{
    /**
     * Is this remote client enabled?
     */
    private boolean enabled = true;

    /**
     * Name of the remote client
     */
    private String name;

    /**
     * The ID of the group that this client belongs to.
     */
    private int groupId;

    /**
     * Should this client be exported to other remote clients
     */
    private boolean export = false;

    /**
     * The network that should be exported to other clients
     * Comma seperated list of CIDR networks
     */
    private String exportNetwork;
    
    public OpenVpnRemoteClient() {}

    public boolean getEnabled() { return this.enabled; }
    public void setEnabled( boolean newValue ) { this.enabled = newValue; }

    public String getName() { return name; }
    public void setName(String name) { this.name = ( name == null ? null : name.replaceAll("\\s","") ); }

    public int getGroupId() { return groupId; }
    public void setGroupId( int newValue ) { this.groupId = newValue; }

    public boolean getExport() { return export; }
    public void setExport( boolean newValue ) { this.export = newValue; }

    public String getExportNetwork() { return exportNetwork; }
    public void setExportNetwork( String newValue ) { this.exportNetwork = newValue; }
}