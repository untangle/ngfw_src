/**
 * $Id: NetworkSettings.java 32043 2012-05-31 21:31:47Z dmorris $
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.util.LinkedList;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Network settings.
 */
@SuppressWarnings("serial")
public class NetworkSettings implements Serializable, JSONString
{
    public NetworkSettings() { }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
