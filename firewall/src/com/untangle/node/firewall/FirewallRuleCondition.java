/**
 * $Id$
 */
package com.untangle.node.firewall;

import com.untangle.uvm.node.RuleCondition;


/**
 * This is a matching criteria for a Firewall Rule
 * Example: "Destination Port" == "80"
 *
 * A FirewallRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class FirewallRuleCondition extends RuleCondition
{
    public FirewallRuleCondition( )
    {
        super();
    }

    public FirewallRuleCondition( ConditionType matcherType, String value )
    {
        super( matcherType, value );
    }

    public FirewallRuleCondition( ConditionType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
