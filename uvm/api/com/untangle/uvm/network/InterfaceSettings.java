/**
 * $Id: InterfaceSettings.java 32043 2012-05-31 21:31:47Z dmorris $
 */
package com.untangle.uvm.network;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.node.IPMaskedAddress;

/**
 * Interface settings.
 */
@SuppressWarnings("serial")
public class InterfaceSettings implements Serializable, JSONString
{
    public Integer interfaceId; /* the ID of the physical interface (1-254) */
    public String name; /* human name: ie External, Internal, Wireless */
    public String physicalDev; /* physical interface name: eth0, etc */
    public String symbolicDev; /* symbolic interface name: eth0, eth0:0, eth0.1, etc */
    public String configType; /* config type: static, dhcp, pppoe, bridged, disabled */
    public boolean isWan; /* is a WAN interface? */

    public InetAddress staticAddress; /* the address  of this interface if configured static, or dhcp override */ 
    public InetAddress staticNetmask; /* the netmask  of this interface if configured static, or dhcp override */
    public InetAddress staticGateway; /* the gateway  of this interface if configured static, or dhcp override */
    public InetAddress staticDns1; /* the dns1  of this interface if configured static, or dhcp override */
    public InetAddress staticDns2; /* the dns2  of this interface if configured static, or dhcp override */

    public List<IPMaskedAddress> aliases; /* alias addresses for static & dhcp */
    
    public InterfaceSettings(int interfaceId, String name, String physicalDev, String symbolicDev, String configType, boolean isWan)
    {
        this.interfaceId = interfaceId;
        this.name = name;
        this.physicalDev = physicalDev;
        this.symbolicDev = symbolicDev;
        this.configType = configType;
        this.isWan = isWan;
    }

    public InterfaceSettings() { }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public Integer getInterfaceId( ) { return this.interfaceId; }
    public void setInterfaceId( Integer interfaceId ) { this.interfaceId = interfaceId; }

    public String getName( ) { return this.name; }
    public void setName( String name ) { this.name = name; }

    public String getPhysicalDev( ) { return this.physicalDev; }
    public void setPhysicalDev( String physicalDev ) { this.physicalDev = physicalDev; }

    public String getSymbolicDev( ) { return this.symbolicDev; }
    public void setSymbolicDev( String symbolicDev ) { this.symbolicDev = symbolicDev; }

    public String getConfigType( ) { return this.configType; }
    public void setConfigType( String configType ) { this.configType = configType; }

    public boolean getIsWan( ) { return this.isWan; }
    public void setIsWan( boolean configType ) { this.isWan = isWan; }

    public InetAddress getStaticAddress( ) { return this.staticAddress; }
    public void setStaticAddress( InetAddress staticAddress ) { this.staticAddress = staticAddress; }

    public InetAddress getStaticNetmask( ) { return this.staticNetmask; }
    public void setStaticNetmask( InetAddress staticNetmask ) { this.staticNetmask = staticNetmask; }

    public InetAddress getStaticGateway( ) { return this.staticGateway; }
    public void setStaticGateway( InetAddress staticGateway ) { this.staticGateway = staticGateway; }

    public InetAddress getStaticDns1( ) { return this.staticDns1; }
    public void setStaticDns1( InetAddress staticDns1 ) { this.staticDns1 = staticDns1; }

    public InetAddress getStaticDns2( ) { return this.staticDns2; }
    public void setStaticDns2( InetAddress staticDns2 ) { this.staticDns2 = staticDns2; }

    public List<IPMaskedAddress> getAliases( ) { return this.aliases; }
    public void setAliases( List<IPMaskedAddress> aliases ) { this.aliases = aliases; }
    
}
