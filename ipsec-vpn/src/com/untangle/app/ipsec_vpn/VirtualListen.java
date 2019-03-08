/**
 * $Id: VirtualListen.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.ipsec_vpn;

import java.io.Serializable;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * This class is used to represent an IP address on which the IPsec daemon will
 * be configured to listen for and accept inbound VPN connections.
 * 
 * @author mahotz
 * 
 */

@SuppressWarnings("serial")
public class VirtualListen implements JSONString, Serializable
{
    private int id;
    private String address;

    public VirtualListen()
    {
    }

    public int getId()
    {
        return (id);
    }

    public String getAddress()
    {
        return (address);
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
