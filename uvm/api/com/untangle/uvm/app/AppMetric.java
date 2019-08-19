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
    public enum Type { COUNTER, AVG_TIME };
    private String name;
    private String displayName;
    private String displayUnits;
    private Long value;
    private Type type;

    private Long timeValue;
    private Long counterValue;

    private boolean expert = false;

    public AppMetric() { }

    public AppMetric(String name, String displayName)
    {
        this.name = name;
        this.displayName = displayName;
        this.value = 0L;
        this.type = Type.COUNTER;
    }

    public AppMetric(String name, String displayName, Long value)
    {
        this.name = name;
        this.displayName = displayName;
        this.value = value;
        this.type = Type.COUNTER;
    }

    public AppMetric(String name, String displayName, Long value, Type type, String displayUnits, boolean expert)
    {
        this.name = name;
        this.displayName = displayName;
        this.value = value;
        this.type = type;
        this.displayUnits = displayUnits;
        this.expert = expert;
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDisplayUnits() { return displayUnits; }
    public void setDisplayUnits(String displayUnits) { this.displayUnits = displayUnits; }

    public Long getValue() { return value;}
    public void setValue( Long value ) { this.value = value; }

    public Type getType() { return type; }
    public void setType(Type name) { this.type = type; }

    public Long getTimeValue() { return timeValue; }
    public void setTimeValue(Long timeValue) { this.timeValue = timeValue; }

    public Long getCounterValue() { return counterValue; }
    public void setCounterValue(Long counterValue) { this.counterValue = counterValue; }

    public boolean getExpert() { return expert; }
    public void setExpertValue(boolean expertValue) { this.expert = expertValue; }

    public Long calculate(){
        if(this.type == Type.AVG_TIME){
            this.setValue(this.timeValue / this.counterValue);
        }

        return value;
    }

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
