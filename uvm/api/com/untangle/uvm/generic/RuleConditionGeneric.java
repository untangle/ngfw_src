/**
 * $Id$
 */
package com.untangle.uvm.generic;

import java.io.Serializable;

import com.untangle.uvm.app.RuleCondition;
import com.untangle.uvm.util.Constants;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * This in the Generic Rule Condition Class
 * Used for vue model transformations
 */
@SuppressWarnings("serial")
public class RuleConditionGeneric implements JSONString, Serializable {

    public RuleConditionGeneric() {}

    public RuleConditionGeneric(String op, RuleCondition.ConditionType type, String value) {
        this.op = op;
        this.type = type;
        this.value = value;
    }

    private String op = Constants.IS_EQUALS_TO;
    private RuleCondition.ConditionType type;
    private String value;

    public String getOp() { return op; }
    public void setOp(String op) { this.op = op; }
    public RuleCondition.ConditionType getType() { return type; }
    public void setType(RuleCondition.ConditionType type) { this.type = type; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
