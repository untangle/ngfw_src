/**
 * $Id$
 */
package com.untangle.uvm.event;

import java.util.LinkedList;
import java.util.List;

import com.untangle.uvm.event.generic.EventRuleActionGeneric;
import com.untangle.uvm.event.generic.EventRuleConditionGeneric;
import com.untangle.uvm.event.generic.EventRuleGeneric;

/**
 * This in the implementation of a Event Rule
 * 
 * A rule is basically a collection of EventRuleConditions and what
 * to do if the conditions match (log, email, etc)
 */
@SuppressWarnings("serial")
public class AlertRule extends EventRule
{
	private Boolean email = false;

    private Boolean emailLimitFrequency = false;
    private Integer emailLimitFrequencyMinutes = 0;

    private long lastEventTime = 0; /* stores the last time this rule sent an event */

    /**
     * Initialize empty instance of AlertRule.
     * @return Empty instance of AlertRule.
     */
    public AlertRule(){}

    /**
     * Initialize instance of AlertRule.
     * @param  enabled                boolean if true, rule is enabled, otherwise disabled.
     * @param  conditions             List of EventRuleCondition to apply to events.
     * @param  log                    boolean if true log the event, otherwise don't log 
     * @param  email                  boolean if true email the event, otherwise don't email
     * @param  description            String description of rule.
     * @param  frequencyLimit         boolean if true, use a frequency limit, otherwise don't.
     * @param  frequencyMinutes       integer of frequency.
     * @param  thresholdEnabled       Boolean if true, look for threshold before acting, otherwise don't.
     * @param  thresholdLimit         Double of threshold limit.
     * @param  thresholdTimeframeSec  Timeframe of threshold in seconds.
     * @param  thresholdGroupingField String of threshold grouping name.
     * @return Instance of AlertRule.
     */
    public AlertRule( boolean enabled, List<EventRuleCondition> conditions, boolean log, boolean email, String description, boolean frequencyLimit, int frequencyMinutes,
                      Boolean thresholdEnabled, Double thresholdLimit, Integer thresholdTimeframeSec, String thresholdGroupingField )
    {
        super(enabled, conditions, log, description, thresholdEnabled,  thresholdLimit, thresholdTimeframeSec, thresholdGroupingField);
        this.setEmail( email );
        this.setEmailLimitFrequency( frequencyLimit );
        this.setEmailLimitFrequencyMinutes( frequencyMinutes );
    }

    /**
     * Initialize instance of AlertRule.
     * @param  enabled                boolean if true, rule is enabled, otherwise disabled.
     * @param  conditions             List of EventRuleCondition to apply to events.
     * @param  log                    boolean if true log the event, otherwise don't log 
     * @param  email                  boolean if true email the event, otherwise don't email
     * @param  description            String description of rule.
     * @param  frequencyLimit         boolean if true, use a frequency limit, otherwise don't.
     * @param  frequencyMinutes       integer of frequency.
     * @return Instance of AlertRule.
     */
    public AlertRule( boolean enabled, List<EventRuleCondition> conditions, boolean log, boolean email, String description, boolean frequencyLimit, int frequencyMinutes )
    {
		this(enabled, conditions, log, email, description, frequencyLimit, frequencyMinutes, false, null, 60, null);
    }

    /**
     * Return email enabled.
     * @return boolean if true send email, otherwise false.
     */
    public Boolean getEmail() { return email; }
    /**
     * Specify email enabled.
     * @param newValue boolean if true send email, otherwise false.
     */
    public void setEmail( Boolean newValue ) { this.email = newValue; }

    /**
     * Return limit by frequency.
     * @return boolean if true process as frequency, otherwise false.
     */
    public Boolean getEmailLimitFrequency() { return emailLimitFrequency; }
    /**
     * Specify limit by frequency.
     * @param newValue boolean if true process as frequency, otherwise false.
     */
    public void setEmailLimitFrequency( Boolean newValue ) { this.emailLimitFrequency = newValue; }

    /**
     * Return frequency minutes.
     * @return Integer of frequency minutes.
     */
    public Integer getEmailLimitFrequencyMinutes() { return emailLimitFrequencyMinutes; }
    /**
     * Sepcify frequency minutes.
     * @param newValue Integer of frequency minutes.
     */
    public void setEmailLimitFrequencyMinutes( Integer newValue ) { this.emailLimitFrequencyMinutes = newValue; }

    /**
     * Return last event time.
     * @return Long timestmp of last time for event for frequency check.
     */
    public long lastEventTime()
    {
        return this.lastEventTime;
    }

    /**
     * Specify last event time with current time stamp.
     */
    public void updateEventTime()
    {
        this.lastEventTime = System.currentTimeMillis();
    }

    /**
     * Determine if frequncy check threshold has been met.
     * @return Boolean if true then threshold has been reached, false otherwise.
     */
    public Boolean frequencyCheck()
    {
        if ( this.getEmailLimitFrequency() && this.getEmailLimitFrequencyMinutes() > 0 ) {
            long currentTime = System.currentTimeMillis();
            long lastEventTime = this.lastEventTime();
            long secondsSinceLastEvent = ( currentTime - lastEventTime ) / 1000;
            // if not enough time has elapsed, just return
            if ( secondsSinceLastEvent < ( this.getEmailLimitFrequencyMinutes() * 60 ) )
            {
                return false;
            }
        }    	
        this.updateEventTime();
        return true;
    }

    /**
     * Transforms a list of AlertRule objects into their generic EventRuleGeneric representation.
     * Used for get api calls
     * @param alertRulesList LinkedList<AlertRule>
     * @return LinkedList<EventRuleGeneric>
     */
    public static LinkedList<EventRuleGeneric> transformAlertRulesToGeneric(LinkedList<AlertRule> alertRulesList) {
        LinkedList<EventRuleGeneric> alertRulesGenList = new LinkedList<>();
        for (AlertRule rule : alertRulesList) {
            EventRuleGeneric alertRuleGen = AlertRule.getAlertRuleGeneric(rule);
            alertRulesGenList.add(alertRuleGen);
        }
        return alertRulesGenList;
    }

    /**
     * Transforms a AlertRule object into its generic EventRuleGeneric representation.
     * @param rule AlertRule
     * @return AlertRule
     */
    private static EventRuleGeneric getAlertRuleGeneric(AlertRule rule) {
        // Transform enabled and ruleId
        boolean enabled = Boolean.TRUE.equals(rule.getEnabled());
        String ruleId = rule.getRuleId() != null ? String.valueOf(rule.getRuleId()) : null;

        // Transform Action
        EventRuleActionGeneric ruleActionGen = new EventRuleActionGeneric();
        ruleActionGen.setType(rule.getEmail() ? EventRuleActionGeneric.Type.EMAIL : EventRuleActionGeneric.Type.EMAIL_OFF);
        ruleActionGen.setEmailLimitFrequency(rule.getEmailLimitFrequency());
        ruleActionGen.setEmailLimitFrequencyMinutes(rule.getEmailLimitFrequencyMinutes());

        // Transform Conditions
        LinkedList<EventRuleConditionGeneric> ruleConditionGenList = new LinkedList<>();
        boolean first = true;
        String className = null;
        for (EventRuleCondition ruleCondition : rule.getConditions()) {
            if (first) {
                className = ruleCondition.getFieldValue();
                first = false;
            } else {
                EventRuleConditionGeneric ruleConditionGen = new EventRuleConditionGeneric(ruleCondition.getComparator(), ruleCondition.getField(), ruleCondition.getFieldValue());
                ruleConditionGenList.add(ruleConditionGen);
            }
        }

        // Create Generic Rule
        EventRuleGeneric filterRulesGen = new EventRuleGeneric(enabled, rule.getDescription(), ruleId);
        // Set Actions
        filterRulesGen.setLog(rule.getLog());
        filterRulesGen.setAction(ruleActionGen);
        // Set Conditions
        filterRulesGen.setClassName(className);
        filterRulesGen.setThresholdEnabled(rule.getThresholdEnabled());
        filterRulesGen.setThresholdGroupingField(rule.getThresholdGroupingField());
        filterRulesGen.setThresholdLimit(rule.getThresholdLimit());
        filterRulesGen.setThresholdTimeframeSec(rule.getThresholdTimeframeSec());
        filterRulesGen.setConditions(ruleConditionGenList);

        return filterRulesGen;
    }

}
