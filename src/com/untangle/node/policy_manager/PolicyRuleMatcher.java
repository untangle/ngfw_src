/**
 * $Id$
 */
package com.untangle.node.policy_manager;

import com.untangle.uvm.node.RuleMatcher;


/**
 * This is a matching criteria for a Policy Rule
 * Example: "Destination Port" == "80"
 *
 * A PolicyRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class PolicyRuleMatcher extends RuleMatcher
{
    public PolicyRuleMatcher( )
    {
        super();
    }

    public PolicyRuleMatcher( MatcherType matcherType, String value )
    {
        super( matcherType, value );
    }

    public PolicyRuleMatcher( MatcherType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
