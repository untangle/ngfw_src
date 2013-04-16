/**
 * $Id: OpenVpnExport.java,v 1.00 2013/04/15 11:24:35 dmorris Exp $
 */
package com.untangle.node.openvpn;

import java.net.InetAddress;

import com.untangle.uvm.node.IPMaskedAddress;

/**
 * A VPN export of address and clients.
 */
@SuppressWarnings("serial")
public class OpenVpnExport implements java.io.Serializable
{
    private boolean enabled = true;

    private String name;
    
    private IPMaskedAddress network;
    
    public OpenVpnExport() { }

    public boolean getEnabled() { return this.enabled; }
    public void setEnabled( boolean newValue ) { this.enabled = newValue; }
    
    public String getName() { return name; }
    public void setName( String newValue ) { this.name = newValue; }

    public IPMaskedAddress getNetwork() { return this.network; }
    public void setNetwork( IPMaskedAddress newValue ) { this.network = newValue; }
}
