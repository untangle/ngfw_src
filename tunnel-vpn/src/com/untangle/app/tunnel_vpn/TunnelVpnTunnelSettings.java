/**
 * $Id$
 */
package com.untangle.app.tunnel_vpn;

import java.net.InetAddress;
    
@SuppressWarnings("serial")
public class TunnelVpnTunnelSettings implements java.io.Serializable
{
    private boolean enabled = true;
    private String name;
    
    public TunnelVpnTunnelSettings() {}

    public boolean getEnabled() { return this.enabled; }
    public void setEnabled( boolean newValue ) { this.enabled = newValue; }

    public String getName() { return name; }
    public void setName(String name) { this.name = ( name == null ? null : name.replaceAll("\\s","") ); }
}
