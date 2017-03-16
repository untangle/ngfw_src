/**
 * $Id$
 */
package com.untangle.app.openvpn;

import java.net.InetAddress;

import com.untangle.uvm.app.IPMaskedAddress;

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
