/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import com.untangle.uvm.util.ValidSerializable;

/**
 * Dns static entry.
 */
@SuppressWarnings("serial")
@ValidSerializable
public class DnsStaticEntry implements Serializable, JSONString
{
    private String name;
    private InetAddress address;
    
    public DnsStaticEntry( String name, InetAddress address)
    {
        this.name = name;
        this.address = address;
    }

    public DnsStaticEntry() {}

    public String getName() { return this.name; }
    public void setName( String newValue ) { this.name = newValue; }

    public InetAddress getAddress() { return this.address; }
    public void setAddress( InetAddress newValue ) { this.address = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}