/*
 * $Id$
 */
package com.untangle.node.firewall;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Settings for the Firewall node.
 */
@SuppressWarnings("serial")
public class FirewallSettings implements Serializable, JSONString
{
    private List<FirewallRule> rules = null;

    private Integer version = Integer.valueOf(1);
    
    public FirewallSettings()
    {
        this.rules = new LinkedList<FirewallRule>();
    }

    public FirewallSettings(List<FirewallRule> rules)
    {
        this.rules = rules;
    }
    
    public List<FirewallRule> getRules()
    {
        return rules;
    }

    public void setRules( List<FirewallRule> rules )
    {
        this.rules = rules;
    }

    public Integer getVersion()
    {
        return this.version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
