/**
 * $Id: RouteRuleRuleMatcher.java,v 1.00 2011/08/24 14:54:43 dmorris Exp $
 */
package com.untangle.uvm.network;

import com.untangle.uvm.node.RuleMatcher;


/**
 * This is a matching criteria for a RouteRule Rule
 * Example: "Destination Port" == "80"
 *
 * A RouteRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class RouteRuleMatcher extends RuleMatcher
{
    public RouteRuleMatcher( )
    {
        super();
    }

    public RouteRuleMatcher( MatcherType matcherType, String value )
    {
        super( matcherType, value );
    }

    public RouteRuleMatcher( MatcherType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
