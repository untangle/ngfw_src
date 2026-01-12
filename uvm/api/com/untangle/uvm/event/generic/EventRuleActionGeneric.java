/**
 * $Id$
 */
package com.untangle.uvm.event.generic;

import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;

/**
 * This in the Generic Rule Action Class for Event Rules
 * Used for vue model transformations
 */
@SuppressWarnings("serial")
public class EventRuleActionGeneric implements JSONString, Serializable {
    /**
     * EMAIL, EMAIL_OFF - Required for Alert Rules
     */
    public enum Type { EMAIL, EMAIL_OFF }

    private EventRuleActionGeneric.Type type;

    public EventRuleActionGeneric.Type getType() { return type; }
    public void setType(EventRuleActionGeneric.Type type) { this.type = type; }

    // Required for Alert Rules
    private Boolean emailLimitFrequency = false;
    private Integer emailLimitFrequencyMinutes = 0;

    public Integer getEmailLimitFrequencyMinutes() { return emailLimitFrequencyMinutes; }
    public void setEmailLimitFrequencyMinutes(Integer emailLimitFrequencyMinutes) { this.emailLimitFrequencyMinutes = emailLimitFrequencyMinutes; }
    public Boolean getEmailLimitFrequency() { return emailLimitFrequency; }
    public void setEmailLimitFrequency(Boolean emailLimitFrequency) { this.emailLimitFrequency = emailLimitFrequency; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
