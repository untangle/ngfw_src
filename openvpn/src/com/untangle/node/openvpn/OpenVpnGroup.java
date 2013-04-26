/**
 * $Id: OpenVpnGroup.java,v 1.00 2013/04/15 11:24:35 dmorris Exp $
 */
package com.untangle.node.openvpn;

import java.net.InetAddress;

import com.untangle.uvm.node.IPMaskedAddress;

/**
 * A VPN group of address and clients.
 */
@SuppressWarnings("serial")
public class OpenVpnGroup implements java.io.Serializable
{
    private int groupId;
    
    private String name;

    private boolean fullTunnel = false;

    private boolean pushDns = false;
    private boolean isDnsOverrideEnabled = false;
    private InetAddress dnsOverride1;
    private InetAddress dnsOverride2;
    
    public OpenVpnGroup() { }

    /**
     * Unique ID of the group
     */
    public int getGroupId() { return groupId; }
    public void setGroupId( int newValue ) { this.groupId = newValue; }

    /**
     * Name of the group
     */
    public String getName() { return name; }
    public void setName( String newValue ) { this.name = newValue; }

    /**
     * Should this group be "full tunnel" (all traffic goes through VPN)
     */
    public boolean getFullTunnel() { return fullTunnel; }
    public void setFullTunnel( boolean fullTunnel ) { this.fullTunnel = fullTunnel; }

    /**
     * Should clients use DNS from the server
     */
    public boolean getPushDns() { return pushDns; }
    public void setPushDns( boolean newValue ) { this.pushDns = newValue; }

    public boolean getIsDnsOverrideEnabled() { return this.isDnsOverrideEnabled; }
    public void setIsDnsOverrideEnabled( boolean newValue ) { this.isDnsOverrideEnabled = newValue; }

    public InetAddress getDnsOverride1() { return this.dnsOverride1; }
    public void setDnsOverride1( InetAddress newValue ) { this.dnsOverride1 = newValue; }

    public InetAddress getDnsOverride2() { return this.dnsOverride2; }
    public void setDnsOverride2( InetAddress newValue ) { this.dnsOverride2 = newValue; }
}
