/**
 * $Id: OpenVpnServer.java,v 1.00 2013/04/15 11:31:16 dmorris Exp $
 */
package com.untangle.node.openvpn;

import java.net.InetAddress;
    
@SuppressWarnings("serial")
public class OpenVpnServer implements java.io.Serializable
{
    private boolean enabled = true;

    private InetAddress address;

    public OpenVpnServer() {}

    public boolean getEnabled() { return this.enabled; }
    public void setEnabled( boolean newValue ) { this.enabled = newValue; }

    public InetAddress getAddress() { return this.address; }
    public void setAddress( InetAddress newValue ) { this.address = newValue; }
}