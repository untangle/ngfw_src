/**
 * $Id$
 */
package com.untangle.app.intrusion_prevention;

import org.json.JSONObject;
import org.json.JSONString;
import java.io.Serializable;

/**
 * Rule condition
 */
@SuppressWarnings("serial")
public class IntrusionPreventionRuleCondition implements Serializable, JSONString
{
    private String type = "";
    private String comparator = "=";
    private String value = "";

    public IntrusionPreventionRuleCondition() { }

    public IntrusionPreventionRuleCondition(String type, String comparator, String value)
    {
        this.comparator = comparator;
        this.type = type;
        this.value = value;
    }

    public String getComparator() { return comparator; }
    public void setComparator(String comparator) { this.comparator = comparator; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}

