/**
 * $Id$
 */

package com.untangle.app.wireguard_vpn;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Settings for the WireguardVpn app.
 */
@SuppressWarnings("serial")
public class WireguardVpnSettings implements Serializable, JSONString
{
    private Integer version = 1;
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
