/**
 * $Id$
 */
package com.untangle.uvm.event;

import java.util.List;

import com.untangle.uvm.event.generic.EventRuleActionGeneric;
import com.untangle.uvm.event.generic.EventRuleConditionGeneric;
import com.untangle.uvm.event.generic.EventRuleGeneric;
import com.untangle.uvm.util.Constants;

import java.util.LinkedList;
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
    private LinkedList<Integer> syslogServers = null;

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


     /**
     * Return Syslog servers
     * @return list of Syslogservers.
     */
    public LinkedList<Integer> getSyslogServers() {
        return syslogServers;
    }

    /**
     * add Syslog servers.
     * @param syslogServers add list of Syslog servers.
     */
    public void setSyslogServers(LinkedList<Integer> syslogServers) {
        this.syslogServers = syslogServers;
    }


    /**
     * Transforms a list of SyslogRule objects into their generic EventRuleGeneric representation.
     * Used for get api calls
     * @param syslogRulesList LinkedList<SyslogRule>
     * @return LinkedList<EventRuleGeneric>
     */
    public static LinkedList<EventRuleGeneric> transformSyslogRulesToGeneric(LinkedList<SyslogRule> syslogRulesList) {
        LinkedList<EventRuleGeneric> syslogRulesGenList = new LinkedList<>();
        for (SyslogRule rule : syslogRulesList) {
            EventRuleGeneric syslogRuleGen = SyslogRule.getSyslogRuleGeneric(rule);
            syslogRulesGenList.add(syslogRuleGen);
        }
        return syslogRulesGenList;
    }

    /**
     * Transforms a SyslogRule object into its generic EventRuleGeneric representation.
     * @param rule SyslogRule
     * @return SyslogRule
     */
    private static EventRuleGeneric getSyslogRuleGeneric(SyslogRule rule) {
        // Transform enabled and ruleId
        boolean enabled = Boolean.TRUE.equals(rule.getEnabled());
        String ruleId = rule.getRuleId() != null ? String.valueOf(rule.getRuleId()) : null;

        // Transform Action
        EventRuleActionGeneric ruleActionGen = new EventRuleActionGeneric();
        ruleActionGen.setType(EventRuleActionGeneric.Type.SYSLOG);
        ruleActionGen.setSyslogServers(rule.getSyslogServers() != null ? rule.getSyslogServers() : new LinkedList<>());
        ruleActionGen.setSyslog(rule.getSyslog() != null ? rule.getSyslog() : false);
        

        // Transform Conditions
        LinkedList<EventRuleConditionGeneric> ruleConditionGenList = new LinkedList<>();
        boolean first = true;
        String className = Constants.ALL;
        if (rule.getConditions() != null) {
            for (EventRuleCondition ruleCondition : rule.getConditions()) {
                if (Constants.CLASS.equals(ruleCondition.getField())) {
                    className = ruleCondition.getFieldValue();
                } else {
                    EventRuleConditionGeneric ruleConditionGen =
                        new EventRuleConditionGeneric(
                            ruleCondition.getComparator(),
                            ruleCondition.getField(),
                            ruleCondition.getFieldValue()
                        );
                    ruleConditionGenList.add(ruleConditionGen);
                }
            }
        }

        // Create Generic Rule
        EventRuleGeneric syslogRulesGen = new EventRuleGeneric(enabled, rule.getDescription(), ruleId);

        // Set Conditions
        syslogRulesGen.setConditions(ruleConditionGenList);
        
        // Set Actions
        syslogRulesGen.setAction(ruleActionGen);
        syslogRulesGen.setClassName(className);
        syslogRulesGen.setLog(rule.getLog());
        syslogRulesGen.setThresholdEnabled(rule.getThresholdEnabled());
        syslogRulesGen.setThresholdTimeframeSec(rule.getThresholdTimeframeSec());
        syslogRulesGen.setThresholdGroupingField((rule.getThresholdGroupingField()));
        syslogRulesGen.setThresholdLimit((rule.getThresholdLimit()));

        return syslogRulesGen;
    }

}
