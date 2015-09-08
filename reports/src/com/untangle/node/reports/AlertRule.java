/**
 * $Id: AlertRule.java,v 1.00 2014/11/05 15:17:53 dmorris Exp $
 */

package com.untangle.node.reports;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeSession;

/**
 * This in the implementation of a Alert Rule
 * 
 * A rule is basically a collection of AlertRuleMatchers (matchers) and what
 * to do if the matchers match (log, alert, or both)
 */
@SuppressWarnings("serial")
public class AlertRule implements JSONString, Serializable
{
    private static final Logger logger = Logger.getLogger( AlertRule.class );

    private Integer ruleId;
    private Boolean enabled;
    private Boolean log;
    private Boolean alert;
    private Boolean alertLimitFrequency = false;
    private Integer alertLimitFrequencyMinutes = 0;
    private String description;

    private long lastAlertTime = 0; /* stores the last time this rule sent an alert */
    
    private List<AlertRuleMatcher> matchers;

    public AlertRule()
    {
    }

    public AlertRule( boolean enabled, List<AlertRuleMatcher> matchers, boolean log, boolean alert, String description, boolean frequencyLimit, int frequencyMinutes )
    {
        this.setEnabled( enabled );
        this.setMatchers( matchers );
        this.setLog( log );
        this.setAlert( alert );
        this.setDescription( description );
        this.setAlertLimitFrequency( frequencyLimit );
        this.setAlertLimitFrequencyMinutes( frequencyMinutes );
    }

    public List<AlertRuleMatcher> getMatchers() { return this.matchers; }
    public void setMatchers( List<AlertRuleMatcher> newValue ) { this.matchers = newValue; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId( Integer newValue ) { this.ruleId = newValue; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean newValue ) { this.enabled = newValue; }

    public Boolean getLog() { return log; }
    public void setLog( Boolean newValue ) { this.log = newValue; }

    public Boolean getAlert() { return alert; }
    public void setAlert( Boolean newValue ) { this.alert = newValue; }

    public Boolean getAlertLimitFrequency() { return alertLimitFrequency; }
    public void setAlertLimitFrequency( Boolean newValue ) { this.alertLimitFrequency = newValue; }

    public Integer getAlertLimitFrequencyMinutes() { return alertLimitFrequencyMinutes; }
    public void setAlertLimitFrequencyMinutes( Integer newValue ) { this.alertLimitFrequencyMinutes = newValue; }
    
    public String getDescription() { return description; }
    public void setDescription( String newValue ) { this.description = newValue; }

    public long lastAlertTime()
    {
        return this.lastAlertTime;
    }

    public void updateAlertTime()
    {
        this.lastAlertTime = System.currentTimeMillis();
    }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public boolean isMatch( JSONObject obj )
    {
        for ( AlertRuleMatcher matcher : matchers ) {
            if ( ! matcher.isMatch ( obj ) )
                return false;
        }
        
        return true;
    }
}
