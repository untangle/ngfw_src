/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Virtual Link List.
 */
@SuppressWarnings("serial")
public class VirtualLink implements Serializable, JSONString
{
    private InetAddress ipAddress;

    public VirtualLink() {}
        
    public InetAddress getIpAddress() { return this.ipAddress; }
    public void setIpAddress( InetAddress newValue ) { this.ipAddress = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}