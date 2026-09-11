/**
 * $Id$
 */
package com.untangle.app.intrusion_prevention.generic;

import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;

/**
 * Generic action for Intrusion Prevention rules, used for Vue UI transformations.
 */
@SuppressWarnings("serial")
public class IntrusionPreventionActionGeneric implements JSONString, Serializable {

    public enum Type { IPS_DEFAULT, IPS_LOG, IPS_BLOCKLOG, IPS_BLOCK, IPS_DISABLE, IPS_WHITELIST }

    private IntrusionPreventionActionGeneric.Type type;
    private String sourceNetworks;
    private String destinationNetworks;

    public IntrusionPreventionActionGeneric.Type getType() { return type; }
    public void setType(IntrusionPreventionActionGeneric.Type type) { this.type = type; }

    public String getSourceNetworks() { return sourceNetworks; }
    public void setSourceNetworks(String sourceNetworks) { this.sourceNetworks = sourceNetworks; }

    public String getDestinationNetworks() { return destinationNetworks; }
    public void setDestinationNetworks(String destinationNetworks) { this.destinationNetworks = destinationNetworks; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
