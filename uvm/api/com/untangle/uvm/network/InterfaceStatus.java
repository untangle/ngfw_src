/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * This object represents the current status/config of an interface.
 * This is not a settings object.
 */
@SuppressWarnings("serial")
public class InterfaceStatus implements Serializable, JSONString
{
    private int     interfaceId;

    private InetAddress v4Address = null;
    private InetAddress v4Netmask = null;
    private InetAddress v4Gateway = null;
    private InetAddress v4Dns1 = null;
    private InetAddress v4Dns2 = null;
    private Integer v4PrefixLength = null;

    private InetAddress v6Address = null;
    private InetAddress v6Gateway = null;
    private Integer v6PrefixLength = null;

    public InterfaceStatus() {}
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
    
    public int getInterfaceId( ) { return this.interfaceId; }
    public void setInterfaceId( int newValue ) { this.interfaceId = newValue; }

    public InetAddress getV4Address( ) { return this.v4Address; }
    public void setV4Address( InetAddress newValue ) { this.v4Address = newValue; }

    public InetAddress getV4Netmask( ) { return this.v4Netmask; }
    public void setV4Netmask( InetAddress newValue ) { this.v4Netmask = newValue; }
    
    public InetAddress getV4Gateway( ) { return this.v4Gateway; }
    public void setV4Gateway( InetAddress newValue ) { this.v4Gateway = newValue; }
    
    public InetAddress getV4Dns1( ) { return this.v4Dns1; }
    public void setV4Dns1( InetAddress newValue ) { this.v4Dns1 = newValue; }

    public InetAddress getV4Dns2( ) { return this.v4Dns2; }
    public void setV4Dns2( InetAddress newValue ) { this.v4Dns2 = newValue; }

    public Integer getV4PrefixLength( ) { return this.v4PrefixLength; }
    public void setV4PrefixLength( Integer newValue ) { this.v4PrefixLength = newValue; }

    public InetAddress getV6Address( ) { return this.v6Address; }
    public void setV6Address( InetAddress newValue ) { this.v6Address = newValue; }

    public Integer getV6PrefixLength( ) { return this.v6PrefixLength; }
    public void setV6PrefixLength( Integer newValue ) { this.v6PrefixLength = newValue; }

    public InetAddress getV6Gateway( ) { return this.v6Gateway; }
    public void setV6Gateway( InetAddress newValue ) { this.v6Gateway = newValue; }
    
}
