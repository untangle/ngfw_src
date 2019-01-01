/**
 * $Id$
 */
package com.untangle.app.intrusion_prevention;

import org.json.JSONObject;
import org.json.JSONString;
import java.io.Serializable;

/**
 * Intrusion prevention rule
 */
@SuppressWarnings("serial")
public class IntrusionPreventionVariable implements Serializable, JSONString
{
    private String name = "";
    private String value = "";

    public IntrusionPreventionVariable() { }

    public IntrusionPreventionVariable(String name, String value)
    {
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}

