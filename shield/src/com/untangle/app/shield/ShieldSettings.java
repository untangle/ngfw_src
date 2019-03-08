/**
 * $Id$
 */
package com.untangle.app.shield;

import java.util.LinkedList;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Settings for the Shield App.
 */
@SuppressWarnings("serial")
public class ShieldSettings implements Serializable, JSONString
{
    private Integer version = null; /* default is set in initializeSettings() */
    private boolean shieldEnabled = true;
    private LinkedList<ShieldRule> rules = new LinkedList<ShieldRule>();
    private int requestPerSecondLimit = 30;
    
    public ShieldSettings() { }

    public Integer getVersion() { return this.version; }
    public void setVersion(Integer newValue) { this.version = newValue; }

    public int getRequestPerSecondLimit() { return this.requestPerSecondLimit; }
    public void setRequestPerSecondLimit( int newValue ) { this.requestPerSecondLimit = newValue; }
    
    public LinkedList<ShieldRule> getRules() { return this.rules; }
    public void setRules( LinkedList<ShieldRule> newValue ) { this.rules = newValue; }

    public boolean isShieldEnabled() { return shieldEnabled; }
    public void setShieldEnabled(boolean shieldEnabled) { this.shieldEnabled = shieldEnabled; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
