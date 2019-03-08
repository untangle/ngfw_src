/**
 * $Id$
 */

package com.untangle.app.openvpn;

import java.net.InetAddress;
import java.util.LinkedList;

/**
 * A group for OpenVPN clients
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class OpenVpnGroup implements java.io.Serializable, org.json.JSONString
{
    private int groupId;
    private String name;
    private boolean fullTunnel = false;
    private boolean pushDns = true;
    private boolean pushDnsSelf = true;
    private InetAddress pushDns1;
    private InetAddress pushDns2;
    private String pushDnsDomain;
    private LinkedList<OpenVpnConfigItem> groupConfigItems = new LinkedList<OpenVpnConfigItem>();

    public OpenVpnGroup()
    {
    }

// THIS IS FOR ECLIPSE - @formatter:off

    public int getGroupId() { return groupId; }
    public void setGroupId( int newValue ) { this.groupId = newValue; }

    public String getName() { return name; }
    public void setName( String newValue ) { this.name = newValue; }

    public boolean getFullTunnel() { return fullTunnel; }
    public void setFullTunnel( boolean fullTunnel ) { this.fullTunnel = fullTunnel; }

    public boolean getPushDns() { return pushDns; }
    public void setPushDns( boolean newValue ) { this.pushDns = newValue; }

    public boolean getPushDnsSelf() { return pushDnsSelf; }
    public void setPushDnsSelf( boolean newValue ) { this.pushDnsSelf = newValue; }
    
    public InetAddress getPushDns1() { return this.pushDns1; }
    public void setPushDns1( InetAddress newValue ) { this.pushDns1 = newValue; }

    public InetAddress getPushDns2() { return this.pushDns2; }
    public void setPushDns2( InetAddress newValue ) { this.pushDns2 = newValue; }

    public String getPushDnsDomain() { return this.pushDnsDomain; }
    public void setPushDnsDomain( String newValue ) { this.pushDnsDomain = newValue; }

    public LinkedList<OpenVpnConfigItem> getGroupConfigItems() { return groupConfigItems; }
    public void setClientConfigItems( LinkedList<OpenVpnConfigItem> argList ) { this.groupConfigItems = argList; }

// THIS IS FOR ECLIPSE - @formatter:on

    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
