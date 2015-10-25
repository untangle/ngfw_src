/**
 * $Id$
 */
package com.untangle.node.bandwidth_control;

import com.untangle.uvm.node.RuleCondition;

/**
 * This is a matching criteria for a Bandwidth Control Rule
 */
@SuppressWarnings("serial")
public class BandwidthControlRuleCondition extends RuleCondition 
{
    private BandwidthControlApp node;
    
    public BandwidthControlRuleCondition( )
    {
        super();
    }
    
    public BandwidthControlRuleCondition( ConditionType matcherType, String value )
    {
        super( matcherType, value );
    }

    public BandwidthControlRuleCondition( ConditionType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
