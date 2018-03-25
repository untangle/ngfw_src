package com.untangle.uvm.event;

import java.util.List;

/**
 * This in the implementation of a Event Rule
 * 
 * A rule is basically a collection of EventRuleConditions and what
 * to do if the conditions match (log, email, etc)
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
