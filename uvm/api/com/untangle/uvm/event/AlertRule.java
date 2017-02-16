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

    public AlertRule()
    {
    }

    public AlertRule( boolean enabled, List<EventRuleCondition> conditions, boolean log, boolean email, String description, boolean frequencyLimit, int frequencyMinutes,
                      Boolean thresholdEnabled, Double thresholdLimit, Integer thresholdTimeframeSec, String thresholdGroupingField )
    {
        super(enabled, conditions, log, email, description, frequencyLimit, frequencyMinutes,
                 	thresholdEnabled,  thresholdLimit, thresholdTimeframeSec, thresholdGroupingField);
        this.setEmail( email );
        this.setLimitFrequency( frequencyLimit );
        this.setLimitFrequencyMinutes( frequencyMinutes );
    }

    public AlertRule( boolean enabled, List<EventRuleCondition> conditions, boolean log, boolean event, String description, boolean frequencyLimit, int frequencyMinutes )
    {
		this(enabled, conditions, log, event, description, frequencyLimit, frequencyMinutes, null, null, null, null);
    }


    public Boolean getEmail() { return email; }
    public void setEmail( Boolean newValue ) { this.email = newValue; }

    public Boolean getLimitFrequency() { return limitFrequency; }
    public void setLimitFrequency( Boolean newValue ) { this.limitFrequency = newValue; }

    public Integer getLimitFrequencyMinutes() { return limitFrequencyMinutes; }
    public void setLimitFrequencyMinutes( Integer newValue ) { this.limitFrequencyMinutes = newValue; }


}