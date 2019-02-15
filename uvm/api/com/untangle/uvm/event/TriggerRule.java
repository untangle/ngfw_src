/**
 * $Id$
 */
package com.untangle.uvm.event;

import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.Tag;
import com.untangle.uvm.app.GlobMatcher;

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
    private static final Logger logger = Logger.getLogger( TriggerRule.class );

    public static enum TriggerAction { TAG_HOST, TAG_DEVICE, TAG_USER, UNTAG_HOST, UNTAG_DEVICE, UNTAG_USER };

    private TriggerAction action;
    private String tagTarget; /* names the JSON entity for the target of the tag */
    private String tagName;
    private Long tagLifetimeSec;

    private GlobMatcher globMatcher;

    /**
     * Initialize empty instance of TriggerRule.
     * @return Empty instance of TriggerRule.
     */
    public TriggerRule(){}

    /**
     * Initialize instance of TriggerRule.
     * @param  enabled                boolean if true, rule is enabled, otherwise disabled.
     * @param  conditions             List of EventRuleCondition to apply to events.
     * @param  log                    boolean if true log the event, otherwise don't log 
     * @param  description            String description of rule.
     * @param  thresholdEnabled       Boolean if true, look for threshold before acting, otherwise don't.
     * @param  thresholdLimit         Double of threshold limit.
     * @param  thresholdTimeframeSec  Timeframe of threshold in seconds.
     * @param  thresholdGroupingField String of threshold grouping name.
     * @return Instance of TriggerRule.
     */
    public TriggerRule( boolean enabled, List<EventRuleCondition> conditions, boolean log, String description, Boolean thresholdEnabled, Double thresholdLimit, Integer thresholdTimeframeSec, String thresholdGroupingField )
    {
        super(enabled, conditions, log, description, thresholdEnabled,  thresholdLimit, thresholdTimeframeSec, thresholdGroupingField);
    }

    /**
     * Initialize instance of TriggerRule.
     * @param  enabled                boolean if true, rule is enabled, otherwise disabled.
     * @param  conditions             List of EventRuleCondition to apply to events.
     * @param  log                    boolean if true log the event, otherwise don't log 
     * @param  description            String description of rule.
     * @param  frequencyLimit         boolean if true, use a frequency limit, otherwise don't.
     * @param  frequencyMinutes       integer of frequency.
     * @return Instance of TriggerRule.
     */
    public TriggerRule( boolean enabled, List<EventRuleCondition> conditions, boolean log, String description, boolean frequencyLimit, int frequencyMinutes )
    {
		this(enabled, conditions, log, description, null, null, null, null);
    }

    /**
     * Return action.
     * @return TriggerAction for this action.
     */
    public TriggerAction getAction() { return this.action; }
    /**
     * Specify action.
     * @param newValue TriggerAction for this action.
     */
    public void setAction( TriggerAction newValue ) { this.action = newValue; }

    /**
     * Return tag target.
     * @return String of tag target.
     */
    public String getTagTarget() { return this.tagTarget; }
    /**
     * Specify tag target.
     * @param newValue String of tag target for this action.
     */
    public void setTagTarget( String newValue ) { this.tagTarget = newValue; }

    /**
     * Return tag name.
     * @return String of tag name.
     */
    public String getTagName() { return this.tagName; }
    /**
     * Specify tag name.
     * @param newValue String of tag name for this action.
     */
    public void setTagName( String newValue )
    {
        this.globMatcher = GlobMatcher.getMatcher( newValue );
        this.tagName = newValue;
    }

    /**
     * Return tag's lifetime.
     * @return long of tag lifetime in seconds.
     */
    public Long getTagLifetimeSec() { return this.tagLifetimeSec; }
    /**
     * Specify tag's lifetime.
     * @param newValue long of tag lifetime in seconds.
     */
    public void setTagLifetimeSec( Long newValue ) { this.tagLifetimeSec = newValue; }
    
    /**
     * If the trigger rules specifies a glob
     * For example UNTAG "foo*"
     * This funciton will tell if you if the supplied tag
     * matches "foo*"
     * @param t Tag contianing name to check match.
     * @return boolean true if name matches, false otherwise.
     */
    public boolean nameMatches( Tag t )
    {
        if ( globMatcher == null ) {
            logger.warn("Missing globMatcher");
            return false;
        }

        return globMatcher.isMatch( t.getName() );
    }
}
