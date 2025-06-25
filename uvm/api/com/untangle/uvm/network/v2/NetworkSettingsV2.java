/**
 * $Id$
 */
package com.untangle.uvm.network.v2;

import java.io.Serializable;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.network.InterfaceSettings;

/**
 * Network settings v2.
 */
@SuppressWarnings("serial")
public class NetworkSettingsV2 implements Serializable, JSONString {

    private List<InterfaceSettingsV2> interfaces = null;

    public NetworkSettingsV2() {
        super();
    }

    public List<InterfaceSettingsV2> getInterfaces() { return interfaces; }
    public void setInterfaces(List<InterfaceSettingsV2> interfaces) { this.interfaces = interfaces; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}