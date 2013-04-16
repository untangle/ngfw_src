/**
 * $Id: OpenVpnNode.java 34022 2013-02-26 19:14:43Z dmorris $
 */
package com.untangle.node.openvpn;

import java.util.List;
import java.net.InetAddress;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.EventLogQuery;

public interface OpenVpnNode extends Node
{
    public enum ConfigFormat
    {
        SETUP_EXE,
        ZIP;
    };
    
    public void setSettings( OpenVpnSettings settings );
    public OpenVpnSettings getSettings();

    /* Returns a URL to use to download the admin key. */
    public String getAdminDownloadLink( String clientName, ConfigFormat format );
     
    public List<OpenVpnStatusEvent> getActiveClients();

    public EventLogQuery[] getStatusEventsQueries();
}
