/**
 * $Id$
 */
package com.untangle.node.wan_balancer;

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
