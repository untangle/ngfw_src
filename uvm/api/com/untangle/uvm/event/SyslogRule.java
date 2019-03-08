/**
 * $Id$
 */
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

    /**
     * Initialize empty instance of EventRuleCondition.
     * @return Empty instance of EventRuleCondition.
     */
    public SyslogRule(){}

    /**
     * Initialize instance of EventRuleCondition.
     * @param  enabled                boolean if true, rule is enabled, otherwise disabled.
     * @param  conditions             List of EventRuleCondition to apply to events.
     * @param  log                    boolean if true log the event, otherwise don't log 
     * @param  syslog                 boolean if true remote syslog the event, otherwise don't syslog
     * @param  description            String description of rule.
     * @param  frequencyLimit         boolean if true, use a frequency limit, otherwise don't.
     * @param  frequencyMinutes       integer of frequency.
     * @param  thresholdEnabled       Boolean if true, look for threshold before acting, otherwise don't.
     * @param  thresholdLimit         Double of threshold limit.
     * @param  thresholdTimeframeSec  Timeframe of threshold in seconds.
     * @param  thresholdGroupingField String of threshold grouping name.
     * @return Instance of EventRuleCondition.
     */
    public SyslogRule( boolean enabled, List<EventRuleCondition> conditions, boolean log, boolean syslog, String description, boolean frequencyLimit, int frequencyMinutes,
                       Boolean thresholdEnabled, Double thresholdLimit, Integer thresholdTimeframeSec, String thresholdGroupingField )
    {
        super(enabled, conditions, log, description, thresholdEnabled,  thresholdLimit, thresholdTimeframeSec, thresholdGroupingField);
        this.setSyslog( syslog );
    }

    /**
     * Initialize instance of EventRuleCondition.
     * @param  enabled                boolean if true, rule is enabled, otherwise disabled.
     * @param  conditions             List of EventRuleCondition to apply to events.
     * @param  log                    boolean if true log the event, otherwise don't log 
     * @param  syslog                 boolean if true remote syslog the event, otherwise don't syslog
     * @param  description            String description of rule.
     * @param  frequencyLimit         boolean if true, use a frequency limit, otherwise don't.
     * @param  frequencyMinutes       integer of frequency.
     * @return Instance of EventRuleCondition.
     */
    public SyslogRule( boolean enabled, List<EventRuleCondition> conditions, boolean log, boolean syslog, String description, boolean frequencyLimit, int frequencyMinutes )
    {
        this(enabled, conditions, log, syslog, description, frequencyLimit, frequencyMinutes, null, null, null, null);
    }

    /**
     * Return syslog enabled.
     * @return boolean if true send via remote syslog, otherwise false.
     */
    public Boolean getSyslog() { return syslog; }
    /**
     * Specify syslog enabled enabled.
     * @param newValue boolean if true send remote syslog, otherwise false.
     */
    public void setSyslog( Boolean newValue ) { this.syslog = newValue; }

}
