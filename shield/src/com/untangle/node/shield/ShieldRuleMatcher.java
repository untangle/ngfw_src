/**
 * $Id: ShieldRuleMatcher.java,v 1.00 2011/08/24 14:54:43 dmorris Exp $
 */
package com.untangle.node.shield;

import com.untangle.uvm.node.RuleMatcher;


/**
 * This is a matching criteria for a Shield Rule
 * Example: "Destination Port" == "80"
 *
 * A ShieldRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class ShieldRuleMatcher extends RuleMatcher
{
    public ShieldRuleMatcher( )
    {
        super();
    }

    public ShieldRuleMatcher( MatcherType matcherType, String value )
    {
        super( matcherType, value );
    }

    public ShieldRuleMatcher( MatcherType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
