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
    public boolean isWan; /* is a WAN interface? */
    public String config; /* config type: addressed, bridged, disabled */

    public String v4ConfigType; /* config type: static, auto, pppoe */
    
    public InetAddress v4StaticAddress; /* the address  of this interface if configured static, or dhcp override */ 
    public InetAddress v4StaticNetmask; /* the netmask  of this interface if configured static, or dhcp override */
    public InetAddress v4StaticGateway; /* the gateway  of this interface if configured static, or dhcp override */
    public InetAddress v4AutoAddressOverride; /* the dhcp override address (null means don't override) */ 
    public InetAddress v4AutoNetmaskOverride; /* the dhcp override netmask (null means don't override) */ 
    public InetAddress v4AutoGatewayOverride; /* the dhcp override gateway (null means don't override) */ 

    public String v6ConfigType; /* config type: static, auto */
    
    public InetAddress v6StaticAddress; /* the address  of this interface if configured static, or dhcp override */ 
    public Integer     v6StaticPrefixLength; /* the netmask  of this interface if configured static, or dhcp override */
    public InetAddress v6StaticGateway; /* the gateway  of this interface if configured static, or dhcp override */

    public InetAddress staticDns1; /* the dns1  of this interface if configured static, or dhcp override */
    public InetAddress staticDns2; /* the dns2  of this interface if configured static, or dhcp override */
    
    public List<IPMaskedAddress> aliases; /* alias addresses for static & dhcp */
    
    public InterfaceSettings(int interfaceId, String name, String physicalDev, String symbolicDev, String config, String v4ConfigType, String v6ConfigType, boolean isWan)
    {
        this.interfaceId = interfaceId;
        this.name = name;
        this.physicalDev = physicalDev;
        this.symbolicDev = symbolicDev;
        this.config = config;
        this.v4ConfigType = v4ConfigType;
        this.v6ConfigType = v6ConfigType;
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

    public boolean getIsWan( ) { return this.isWan; }
    public void setIsWan( boolean isWan ) { this.isWan = isWan; }

    public String getConfig( ) { return this.config; }
    public void setConfig( String config ) { this.config = config; }

    public String getV4ConfigType( ) { return this.v4ConfigType; }
    public void setV4ConfigType( String v4ConfigType ) { this.v4ConfigType = v4ConfigType; }

    public InetAddress getv4StaticAddress( ) { return this.v4StaticAddress; }
    public void setv4StaticAddress( InetAddress v4StaticAddress ) { this.v4StaticAddress = v4StaticAddress; }

    public InetAddress getV4StaticNetmask( ) { return this.v4StaticNetmask; }
    public void setv4StaticNetmask( InetAddress v4StaticNetmask ) { this.v4StaticNetmask = v4StaticNetmask; }
    
    public InetAddress getV4StaticGateway( ) { return this.v4StaticGateway; }
    public void setv4StaticGateway( InetAddress v4StaticGateway ) { this.v4StaticGateway = v4StaticGateway; }
    
    public InetAddress getv4AutoAddressOverride( ) { return this.v4AutoAddressOverride; }
    public void setv4AutoAddressOverride( InetAddress v4AutoAddressOverride ) { this.v4AutoAddressOverride = v4AutoAddressOverride; }
    
    public InetAddress getV4AutoNetmaskOverride( ) { return this.v4AutoNetmaskOverride; }
    public void setv4AutoNetmaskOverride( InetAddress v4AutoNetmaskOverride ) { this.v4AutoNetmaskOverride = v4AutoNetmaskOverride; }
    
    public InetAddress getV4AutoGatewayOverride( ) { return this.v4AutoGatewayOverride; }
    public void setv4AutoGatewayOverride( InetAddress v4AutoGatewayOverride ) { this.v4AutoGatewayOverride = v4AutoGatewayOverride; }
    
    public String getV6ConfigType( ) { return this.v6ConfigType; }
    public void setV6ConfigType( String v6ConfigType ) { this.v6ConfigType = v6ConfigType; }

    public InetAddress getv6StaticAddress( ) { return this.v6StaticAddress; }
    public void setv6StaticAddress( InetAddress v6StaticAddress ) { this.v6StaticAddress = v6StaticAddress; }

    public Integer getV6StaticPrefixLength( ) { return this.v6StaticPrefixLength; }
    public void setV6StaticPrefixLength( Integer v6StaticNetmask ) { this.v6StaticPrefixLength = v6StaticPrefixLength; }

    public InetAddress getV6StaticGateway( ) { return this.v6StaticGateway; }
    public void setV6StaticGateway( InetAddress v6StaticGateway ) { this.v6StaticGateway = v6StaticGateway; }

    public InetAddress getStaticDns1( ) { return this.staticDns1; }
    public void setStaticDns1( InetAddress staticDns1 ) { this.staticDns1 = staticDns1; }

    public InetAddress getStaticDns2( ) { return this.staticDns2; }
    public void setStaticDns2( InetAddress staticDns2 ) { this.staticDns2 = staticDns2; }

    public List<IPMaskedAddress> getAliases( ) { return this.aliases; }
    public void setAliases( List<IPMaskedAddress> aliases ) { this.aliases = aliases; }
    
}
