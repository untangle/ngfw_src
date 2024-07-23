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
 * Dns local server.
 */
@SuppressWarnings("serial")
@ValidSerializable
public class DnsLocalServer implements Serializable, JSONString
{
    private String domain;
    private InetAddress localServer;
    
    public DnsLocalServer( String domain, InetAddress localServer)
    {
        this.domain = domain;
        this.localServer = localServer;
    }

    public DnsLocalServer() {}

    public String getDomain() { return this.domain; }
    public void setDomain( String newValue ) { this.domain = newValue; }

    public InetAddress getLocalServer() { return this.localServer; }
    public void setLocalServer( InetAddress newValue ) { this.localServer = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}