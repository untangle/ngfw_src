/**
 * $Id: FirewallRuleMatcher.java,v 1.00 2011/08/24 14:54:43 dmorris Exp $
 */
package com.untangle.node.firewall;

import com.untangle.uvm.node.RuleMatcher;


/**
 * This is a matching criteria for a Firewall Rule
 * Example: "Destination Port" == "80"
 *
 * A FirewallRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class FirewallRuleMatcher extends RuleMatcher
{
    public FirewallRuleMatcher( )
    {
        super();
    }

    public FirewallRuleMatcher( MatcherType matcherType, String value )
    {
        super( matcherType, value );
    }

    public FirewallRuleMatcher( MatcherType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
