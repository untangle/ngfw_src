package com.untangle.uvm.event;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.util.Load;

/**
 * This in the implementation of a Event Rule
 * 
 * A rule is basically a collection of EventRuleConditions (matchers) and what
 * to do if the matchers match (log, email, etc)
 */
@SuppressWarnings("serial")
public class AlertRule extends EventRule
{
	private Boolean email = false;

    private Boolean limitFrequency = false;
    private Integer limitFrequencyMinutes = 0;

    private long lastEventTime = 0; /* stores the last time this rule sent an event */

    public AlertRule()
    {
    }

    public AlertRule( boolean enabled, List<EventRuleCondition> conditions, boolean log, boolean email, String description, boolean frequencyLimit, int frequencyMinutes,
                      Boolean thresholdEnabled, Double thresholdLimit, Integer thresholdTimeframeSec, String thresholdGroupingField )
    {
        super(enabled, conditions, log, description, thresholdEnabled,  thresholdLimit, thresholdTimeframeSec, thresholdGroupingField);
        this.setEmail( email );
        this.setLimitFrequency( frequencyLimit );
        this.setLimitFrequencyMinutes( frequencyMinutes );
    }

    public AlertRule( boolean enabled, List<EventRuleCondition> conditions, boolean log, boolean email, String description, boolean frequencyLimit, int frequencyMinutes )
    {
		this(enabled, conditions, log, email, description, frequencyLimit, frequencyMinutes, null, null, null, null);
    }


    public Boolean getEmail() { return email; }
    public void setEmail( Boolean newValue ) { this.email = newValue; }

    public Boolean getLimitFrequency() { return limitFrequency; }
    public void setLimitFrequency( Boolean newValue ) { this.limitFrequency = newValue; }

    public Integer getLimitFrequencyMinutes() { return limitFrequencyMinutes; }
    public void setLimitFrequencyMinutes( Integer newValue ) { this.limitFrequencyMinutes = newValue; }

    public long lastEventTime()
    {
        return this.lastEventTime;
    }

    public void updateEventTime()
    {
        this.lastEventTime = System.currentTimeMillis();
    }
    
    public Boolean frequencyCheck()
    {
        if ( this.getLimitFrequency() && this.getLimitFrequencyMinutes() > 0 ) {
            long currentTime = System.currentTimeMillis();
            long lastEventTime = this.lastEventTime();
            long secondsSinceLastEvent = ( currentTime - lastEventTime ) / 1000;
            // if not enough time has elapsed, just return
            if ( secondsSinceLastEvent < ( this.getLimitFrequencyMinutes() * 60 ) )
            {
                return false;
            }
        }    	
        this.updateEventTime();
        return true;
    }

}