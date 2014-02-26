/**
 * $Id$
 */
package com.untangle.uvm.network;

import com.untangle.uvm.node.RuleMatcher;


/**
 * This is a matching criteria for a BypassRule Rule
 * Example: "Destination Port" == "80"
 *
 * A BypassRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class BypassRuleMatcher extends RuleMatcher
{
    public BypassRuleMatcher( )
    {
        super();
    }

    public BypassRuleMatcher( MatcherType matcherType, String value )
    {
        super( matcherType, value );
    }

    public BypassRuleMatcher( MatcherType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
