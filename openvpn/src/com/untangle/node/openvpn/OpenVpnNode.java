/**
 * $Id: OpenVpnNode.java 34022 2013-02-26 19:14:43Z dmorris $
 */
package com.untangle.node.openvpn;

import java.util.List;
import java.net.InetAddress;

import org.json.JSONObject;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.EventLogQuery;

public interface OpenVpnNode extends Node
{
    public void setSettings( OpenVpnSettings settings );
    public OpenVpnSettings getSettings();

    public EventLogQuery[] getStatusEventsQueries();

    public String getClientDistributionDownloadLink( String clientName, String format );

    public String getClientDistributionUploadLink( );
     
    public List<OpenVpnStatusEvent> getActiveClients();

    public List<JSONObject> getRemoteServersStatus();
}
