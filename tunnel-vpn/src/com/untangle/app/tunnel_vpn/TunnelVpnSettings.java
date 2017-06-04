/*
 * $Id$
 */
package com.untangle.app.tunnel_vpn;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Settings for the TunnelVpn app.
 */
@SuppressWarnings("serial")
public class TunnelVpnSettings implements Serializable, JSONString
{
    private Integer version = Integer.valueOf(1);
    
    public TunnelVpnSettings() {}

    public Integer getVersion() { return this.version; }
    public void setVersion(Integer newValue) { this.version = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
