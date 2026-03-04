/**
 * $Id$
 */
package com.untangle.app.shield.generic;

import java.util.LinkedList;

import java.io.Serializable;

import com.untangle.app.shield.ShieldRule;
import com.untangle.app.shield.ShieldSettings;
import com.untangle.uvm.generic.RuleGeneric;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * Generic Settings for the Shield App.
 */
@SuppressWarnings("serial")
public class ShieldSettingsGeneric implements Serializable, JSONString {
    private boolean enabled = true;
    private int requestPerSecondLimit = 30;
    private LinkedList<RuleGeneric> shield_rules = new LinkedList<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getRequestPerSecondLimit() { return requestPerSecondLimit; }
    public void setRequestPerSecondLimit(int requestPerSecondLimit) { this.requestPerSecondLimit = requestPerSecondLimit; }
    public LinkedList<RuleGeneric> getShield_rules() { return shield_rules; }
    public void setShield_rules(LinkedList<RuleGeneric> shield_rules) { this.shield_rules = shield_rules; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms a {@link ShieldSettingsGeneric} object into its v1 ShieldSettings representation.
     * @param shieldSettings ShieldSettings
     * @return ShieldSettings
     */
    public ShieldSettings transformGenericToQosSettings(ShieldSettings shieldSettings) {
        if (shieldSettings == null)
            shieldSettings = new ShieldSettings();

        shieldSettings.setShieldEnabled(this.isEnabled());
        shieldSettings.setRequestPerSecondLimit(this.getRequestPerSecondLimit());
        // Set Shield Rules
        if (this.getShield_rules() != null)
            shieldSettings.setRules(ShieldRule.transformGenericToShieldRules(this.getShield_rules(), shieldSettings.getRules()));

        return shieldSettings;
    }
}
