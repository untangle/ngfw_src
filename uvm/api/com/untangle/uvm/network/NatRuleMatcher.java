/**
 * $Id: NatRuleRuleMatcher.java,v 1.00 2011/08/24 14:54:43 dmorris Exp $
 */
package com.untangle.uvm.network;

import com.untangle.uvm.node.RuleMatcher;


/**
 * This is a matching criteria for a NatRule Rule
 * Example: "Destination Port" == "80"
 *
 * A NatRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class NatRuleMatcher extends RuleMatcher
{
    public NatRuleMatcher( )
    {
        super();
    }

    public NatRuleMatcher( MatcherType matcherType, String value )
    {
        super( matcherType, value );
    }

    public NatRuleMatcher( MatcherType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
