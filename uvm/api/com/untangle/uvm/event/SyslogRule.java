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
public class SyslogRule extends EventRule
{
	private Boolean syslog = false;

    public SyslogRule()
    {
    }

    public SyslogRule( boolean enabled, List<EventRuleCondition> conditions, boolean log, boolean syslog, String description, boolean frequencyLimit, int frequencyMinutes,
                      Boolean thresholdEnabled, Double thresholdLimit, Integer thresholdTimeframeSec, String thresholdGroupingField )
    {
        super(enabled, conditions, log, description, thresholdEnabled,  thresholdLimit, thresholdTimeframeSec, thresholdGroupingField);
        this.setSyslog( syslog );
    }
    public SyslogRule( boolean enabled, List<EventRuleCondition> conditions, boolean log, boolean syslog, String description, boolean frequencyLimit, int frequencyMinutes )
    {
        this(enabled, conditions, log, syslog, description, frequencyLimit, frequencyMinutes, null, null, null, null);
    }

    public Boolean getSyslog() { return syslog; }
    public void setSyslog( Boolean newValue ) { this.syslog = newValue; }

}