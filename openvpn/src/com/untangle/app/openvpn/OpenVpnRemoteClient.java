/**
 * $Id$
 */

package com.untangle.app.openvpn;

import java.util.LinkedList;

/**
 * Class to represent an OpenVPN remote client
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class OpenVpnRemoteClient implements java.io.Serializable, org.json.JSONString
{
    private boolean enabled = true;
    private String name;
    private int groupId;
    private boolean export = false;
    private String exportNetwork;

    private LinkedList<OpenVpnConfigItem> clientConfigItems = new LinkedList<OpenVpnConfigItem>();

    public OpenVpnRemoteClient()
    {
    }

// THIS IS FOR ECLIPSE - @formatter:off

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

// THIS IS FOR ECLIPSE - @formatter:on

    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
