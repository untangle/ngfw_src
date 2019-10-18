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

    private Integer reputationThreshold = 40;
    private String action = ThreatPreventionApp.ACTION_BLOCK;
    private Boolean flag = true;

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

    public Integer getReputationThreshold() { return this.reputationThreshold; }
    public void setReputationThreshold(Integer newValue) { this.reputationThreshold = newValue; }

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
