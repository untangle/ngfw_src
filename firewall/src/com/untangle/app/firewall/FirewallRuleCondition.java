/**
 * $Id$
 */
package com.untangle.app.firewall;

import com.untangle.uvm.app.RuleCondition;


/**
 * This is a matching criteria for a Firewall Rule
 * Example: "Destination Port" == "80"
 *
 * A FirewallRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class FirewallRuleCondition extends RuleCondition
{
    /**
     * Create a firewall rule condition
     */
    public FirewallRuleCondition( )
    {
        super();
    }

    /**
     * Create a firewall rule condition
     * @param matcherType
     * @param value
     */
    public FirewallRuleCondition( ConditionType matcherType, String value )
    {
        super( matcherType, value );
    }

    /**
     * Create a firewall rule condition
     * @param matcherType
     * @param value
     * @param invert
     */
    public FirewallRuleCondition( ConditionType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
