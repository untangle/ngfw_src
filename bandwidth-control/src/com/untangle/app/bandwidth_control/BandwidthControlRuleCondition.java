/**
 * $Id$
 */
package com.untangle.app.bandwidth_control;

import com.untangle.uvm.app.RuleCondition;

/**
 * This is a matching criteria for a Bandwidth Control Rule
 * It does not extend the basic RuleCondition in any way
 * It is its only object just for type verification
 */
@SuppressWarnings("serial")
public class BandwidthControlRuleCondition extends RuleCondition 
{
    private BandwidthControlApp app;
    
    /**
     * Instantiate a BandwidthControlRuleCondition
     */
    public BandwidthControlRuleCondition( )
    {
        super();
    }
    
    /**
     * Instantiate a BandwidthControlRuleCondition
     * @param conditionType - the type of condition
     * @param value - the value of the conditionType
     */
    public BandwidthControlRuleCondition( ConditionType conditionType, String value )
    {
        super( conditionType, value );
    }

    /**
     * Instantiate a BandwidthControlRuleCondition
     * @param conditionType - the type of condition
     * @param value - the value of the conditionType
     * @param invert - if true means "not equal" instead of equal
     */
    public BandwidthControlRuleCondition( ConditionType conditionType, String value, Boolean invert )
    {
        super( conditionType, value, invert );
    }
}
