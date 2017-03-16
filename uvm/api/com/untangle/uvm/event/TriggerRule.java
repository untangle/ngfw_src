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

import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.util.Load;

import com.untangle.uvm.event.EventRule;

/**
 * This in the implementation of a Event Rule
 * 
 * A rule is basically a collection of EventRuleConditions and what
 * to do if the conditions match
 */
@SuppressWarnings("serial")
public class TriggerRule extends EventRule
{
    public static enum TriggerAction { TAG_HOST, TAG_DEVICE, TAG_USER };

    private TriggerAction action;
    private String tagTarget; /* names the JSON entity for the target of the tag */
    private String tagName;
    private Long tagLifetimeSec;
    
    public TriggerRule()
    {
    }

    public TriggerRule( boolean enabled, List<EventRuleCondition> conditions, boolean log, String description, 
                        Boolean thresholdEnabled, Double thresholdLimit, Integer thresholdTimeframeSec, String thresholdGroupingField )
    {
        super(enabled, conditions, log, description, thresholdEnabled,  thresholdLimit, thresholdTimeframeSec, thresholdGroupingField);
    }

    public TriggerRule( boolean enabled, List<EventRuleCondition> conditions, boolean log, String description, boolean frequencyLimit, int frequencyMinutes )
    {
		this(enabled, conditions, log, description, null, null, null, null);
    }

    public TriggerAction getAction() { return this.action; }
    public void setAction( TriggerAction newValue ) { this.action = newValue; }

    public String getTagTarget() { return this.tagTarget; }
    public void setTagTarget( String newValue ) { this.tagTarget = newValue; }

    public String getTagName() { return this.tagName; }
    public void setTagName( String newValue ) { this.tagName = newValue; }

    public Long getTagLifetimeSec() { return this.tagLifetimeSec; }
    public void setTagLifetimeSec( Long newValue ) { this.tagLifetimeSec = newValue; }
    
    
}
