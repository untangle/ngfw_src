/**
 * $Id$
 */
package com.untangle.app.ip_reputation;

import com.untangle.uvm.app.RuleCondition;


/**
 * This is a matching criteria for a IpReputationPass Rule
 * Example: "Destination Port" == "80"
 *
 * A IpReputationPassRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class IpReputationPassRuleCondition extends RuleCondition
{
    /**
     * Create a firewall rule condition
     */
    public IpReputationPassRuleCondition( )
    {
        super();
    }

    /**
     * Create a firewall rule condition
     * @param matcherType
     * @param value
     */
    public IpReputationPassRuleCondition( ConditionType matcherType, String value )
    {
        super( matcherType, value );
    }

    /**
     * Create a firewall rule condition
     * @param matcherType
     * @param value
     * @param invert
     */
    public IpReputationPassRuleCondition( ConditionType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
