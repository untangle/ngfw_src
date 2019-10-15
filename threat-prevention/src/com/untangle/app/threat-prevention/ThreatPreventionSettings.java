/**
 * $Id$
 */
package com.untangle.app.threat_prevention;

import org.apache.log4j.Logger;


import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.lang.Math;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Settings for the Threat Prevention application.
 */
@SuppressWarnings("serial")
public class ThreatPreventionSettings implements Serializable, JSONString
{
    private final Logger logger = Logger.getLogger(getClass());
    private Integer version = Integer.valueOf(1);

    private Integer threatLevel = 60;
    private String action = "block";
    private Boolean flag = false;

    private Integer threatMask = null;

    private List<ThreatPreventionRule> rules = null;
    
    public ThreatPreventionSettings()
    {
        this.rules = new LinkedList<>();
    }

    public ThreatPreventionSettings(List<ThreatPreventionRule> rules)
    {
        this.rules = rules;
    }
    
    public Integer getVersion() { return this.version; }
    public void setVersion(Integer newValue) { this.version = newValue; }

    public Integer getThreatLevel() { return this.threatLevel; }
    public void setThreatLevel(Integer newValue) { this.threatLevel = newValue; }

    public String getAction() { return this.action; }
    public void setAction(String newValue) { this.action = newValue; }

    public Boolean getFlag() { return flag; }
    public void setFlag( Boolean newValue ) { this.flag = newValue; }

    public List<ThreatPreventionRule> getRules() { return rules; }
    public void setRules( List<ThreatPreventionRule> newValue ) { this.rules = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}
