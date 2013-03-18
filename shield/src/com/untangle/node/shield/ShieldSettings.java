/*
 * $Id$
 */
package com.untangle.node.shield;

import java.util.LinkedList;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Settings for the Shield Node.
 */
@SuppressWarnings("serial")
public class ShieldSettings implements java.io.Serializable
{
    private LinkedList<ShieldRule> rules = new LinkedList<ShieldRule>();

    public ShieldSettings() { }

    /**
     * Shield node configuration rules.
     *
     * @return the set of user settings
     */
    public LinkedList<ShieldRule> getRules()
    {
        return this.rules;
    }

    public void setRules(LinkedList<ShieldRule> rules)
    {
        if (rules == null) {
            rules = new LinkedList<ShieldRule>();
        }

        this.rules = rules;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}
