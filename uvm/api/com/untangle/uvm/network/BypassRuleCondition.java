/**
 * $Id$
 */
package com.untangle.uvm.network;

import com.untangle.uvm.app.RuleCondition;


/**
 * This is a matching criteria for a BypassRule Rule
 * Example: "Destination Port" == "80"
 *
 * A BypassRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class BypassRuleCondition extends RuleCondition
{
    public BypassRuleCondition( )
    {
        super();
    }

    public BypassRuleCondition( ConditionType matcherType, String value )
    {
        super( matcherType, value );
    }

    public BypassRuleCondition( ConditionType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
