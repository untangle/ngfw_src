/**
 * $Id: OpenVpnRemoteServer.java,v 1.00 2013/04/15 11:31:16 dmorris Exp $
 */
package com.untangle.node.openvpn;

import java.net.InetAddress;
    
@SuppressWarnings("serial")
public class OpenVpnRemoteServer implements java.io.Serializable
{
    /**
     * Is this remote server enabled?
     */
    private boolean enabled = true;

    /**
     * Name of the remote server
     */
    private String name;
    
    private InetAddress address;

    public OpenVpnRemoteServer() {}

    public boolean getEnabled() { return this.enabled; }
    public void setEnabled( boolean newValue ) { this.enabled = newValue; }

    public String getName() { return name; }
    public void setName(String name) { this.name = ( name == null ? null : name.replaceAll("\\s","") ); }
    
    public InetAddress getAddress() { return this.address; }
    public void setAddress( InetAddress newValue ) { this.address = newValue; }
}