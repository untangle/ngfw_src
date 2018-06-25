/**
 * $Id$
 */

package com.untangle.uvm.app;

import java.io.Serializable;
import org.json.JSONString;

/**
 * Class to manage an application metric
 */
@SuppressWarnings("serial")
public class AppMetric implements Serializable, JSONString
{
    private String name;
    private String displayName;
    private Long value;
    
    public AppMetric() { }

    public AppMetric(String name, String displayName)
    {
        this.name = name;
        this.displayName = displayName;
        this.value = 0L;
    }

    public AppMetric(String name, String displayName, Long value)
    {
        this.name = name;
        this.displayName = displayName;
        this.value = value;
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public Long getValue() { return value; }
    public void setValue( Long value ) { this.value = value; }

    public String toString()
    {
        return "AppMetric[" + name + "] = " + value;
    }
    
    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
