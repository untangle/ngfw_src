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
    private String threats = "1,2,3,4,5,6,7,8,9,12,14";
    private String action = "block";
    private Boolean flag = false;

    private Integer threatMask = null;

    private List<ThreatPreventionPassRule> passRules = null;
    
    public ThreatPreventionSettings()
    {
        this.passRules = new LinkedList<>();
    }

    public ThreatPreventionSettings(List<ThreatPreventionPassRule> passRules)
    {
        this.passRules = passRules;
    }
    
    public Integer getVersion() { return this.version; }
    public void setVersion(Integer newValue) { this.version = newValue; }

    public Integer getThreatLevel() { return this.threatLevel; }
    public void setThreatLevel(Integer newValue) { this.threatLevel = newValue; }

    public String getThreats() { return this.threats; }
    public void setThreats(String newValue) { 
        this.threats = newValue;
        this.threatMask = null;
    }

    public String getAction() { return this.action; }
    public void setAction(String newValue) { this.action = newValue; }

    public Boolean getFlag() { return flag; }
    public void setFlag( Boolean newValue ) { this.flag = newValue; }

    public List<ThreatPreventionPassRule> getPassRules() { return passRules; }
    public void setPassRules( List<ThreatPreventionPassRule> newValue ) { this.passRules = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public Integer getThreatMask(){
        if(this.threatMask == null){
            boolean any = false;
            String[] bits = threats.split(",");
            this.threatMask = 0;
            for(String bit : bits){
                if(bit.equals("0")){
                    any = true;
                }
                this.threatMask += (int) Math.pow(2,Integer.parseInt(bit));
            }
            if(any == true){
                this.threatMask = Integer.MAX_VALUE;
            }
        }
        return this.threatMask;
    }
}
