/**
 * $Id: IpsecVpnNetwork.java 39842 2015-03-11 15:50:17Z mahotz $
 */

package com.untangle.app.ipsec_vpn;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * This class is used to hold the settings for GRE networks. Yes, it probably
 * should have been called IpsecGreNetwork, but changing it now would require
 * adding cruft to rename the class in existing settings, so I decided to just
 * add this super helpful comment instead.
 * 
 * @author mahotz
 * 
 */

@SuppressWarnings("serial")
public class IpsecVpnNetwork implements JSONString, Serializable
{
    private int id;
    private boolean active;
    private String description;
    private String localAddress;
    private String remoteAddress;
    private String remoteNetworks;
    private int ttl = 64;
    private int mtu = 1476;

    public IpsecVpnNetwork()
    {
    }

    // THIS IS FOR ECLIPSE - @formatter:off

    public int getId() { return (id); }
    public void setId(int id) { this.id = id; }

    public boolean getActive() { return (active); }
    public void setActive(boolean active) { this.active = active; }

    public String getDescription() { return (description); }
    public void setDescription(String description) { this.description = description; }

    public String getLocalAddress() { return (localAddress); }
    public void setLocalAddress(String localAddress) { this.localAddress = localAddress; }

    public String getRemoteAddress() { return (remoteAddress); }
    public void setRemoteAddress(String remoteAddress) { this.remoteAddress = remoteAddress; }

    public String getRemoteNetworks() { return (remoteNetworks); }
    public void setRemoteNetworks(String remoteNetworks) { this.remoteNetworks = remoteNetworks; }

    public int getTtl() { return (ttl); }
    public void setTtl(int value) { this.ttl = value; }

    public int getMtu() { return (mtu); }
    public void setMtu(int value) { this.mtu = value; }

    // THIS IS FOR ECLIPSE - @formatter:on

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
