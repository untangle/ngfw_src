/**
 * $Id$
 */
package com.untangle.uvm.event.generic;

import com.untangle.uvm.app.RuleCondition;
import com.untangle.uvm.util.Constants;
import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;

/**
 * This in the Generic Rule Condition Class for Event Rules
 * Used for vue model transformations
 */
@SuppressWarnings("serial")
public class EventRuleConditionGeneric implements JSONString, Serializable {

    public EventRuleConditionGeneric() {}

    public EventRuleConditionGeneric(String op, String type, String value) {
        this.op = op;
        this.type = type;
        this.value = value;
    }

    private String op = Constants.IS_EQUALS_TO;
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
