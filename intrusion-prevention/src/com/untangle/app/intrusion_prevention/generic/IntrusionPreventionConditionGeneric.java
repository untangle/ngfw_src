/**
 * $Id$
 */
package com.untangle.app.intrusion_prevention.generic;

import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;

/**
 * Generic condition for Intrusion Prevention rules, used for Vue UI transformations.
 */
@SuppressWarnings("serial")
public class IntrusionPreventionConditionGeneric implements JSONString, Serializable {

    public IntrusionPreventionConditionGeneric() {}

    public IntrusionPreventionConditionGeneric(String op, String type, String value) {
        this.op = op;
        this.type = type;
        this.value = value;
    }

    private String op = "=";
    private String type;
    private String value;

    public String getOp() { return op; }
    public void setOp(String op) { this.op = op; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
