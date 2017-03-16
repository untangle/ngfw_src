/**
 * $Id$
 */
package com.untangle.app.openvpn;

import java.net.InetAddress;

import com.untangle.uvm.app.IPMaskedAddress;

/**
 * A VPN group of address and clients.
 */
@SuppressWarnings("serial")
public class OpenVpnGroup implements java.io.Serializable
{
    private int groupId;
    
    private String name;

    private boolean fullTunnel = false;

    private boolean pushDns = true;
    private boolean pushDnsSelf = true;
    private InetAddress pushDns1;
    private InetAddress pushDns2;
    private String pushDnsDomain;
    
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
     * Should the server push DNS config to the clients
     */
    public boolean getPushDns() { return pushDns; }
    public void setPushDns( boolean newValue ) { this.pushDns = newValue; }

    /**
     * Should clients use DNS from the server itself
     */
    public boolean getPushDnsSelf() { return pushDnsSelf; }
    public void setPushDnsSelf( boolean newValue ) { this.pushDnsSelf = newValue; }
    
    public InetAddress getPushDns1() { return this.pushDns1; }
    public void setPushDns1( InetAddress newValue ) { this.pushDns1 = newValue; }

    public InetAddress getPushDns2() { return this.pushDns2; }
    public void setPushDns2( InetAddress newValue ) { this.pushDns2 = newValue; }

    public String getPushDnsDomain() { return this.pushDnsDomain; }
    public void setPushDnsDomain( String newValue ) { this.pushDnsDomain = newValue; }
    

}
