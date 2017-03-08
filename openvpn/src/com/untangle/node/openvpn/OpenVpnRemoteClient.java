/**
 * $Id$
 */
package com.untangle.node.openvpn;

import java.net.InetAddress;
import java.util.LinkedList;

import com.untangle.uvm.node.IPMaskedAddress;

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

    /**
     * A list of config items unique to this client
     */
    private LinkedList<OpenVpnConfigItem> clientConfigItems = new LinkedList<OpenVpnConfigItem>();
    
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

    public LinkedList<OpenVpnConfigItem> getClientConfigItems() { return clientConfigItems; }
    public void setClientConfigItems( LinkedList<OpenVpnConfigItem> argList ) { this.clientConfigItems = argList; }
}
