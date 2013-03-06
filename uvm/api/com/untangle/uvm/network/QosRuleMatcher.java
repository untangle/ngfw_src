/**
 * $Id: QosRuleMatcher.java,v 1.00 2011/08/24 14:54:43 dmorris Exp $
 */
package com.untangle.uvm.network;

import com.untangle.uvm.node.RuleMatcher;

/**
 * This is a matching criteria for a Qos Rule
 * Example: "Destination Port" == "80"
 *
 * A QosRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class QosRuleMatcher extends RuleMatcher
{
    public QosRuleMatcher( )
    {
        super();
    }

    public QosRuleMatcher( MatcherType matcherType, String value )
    {
        super( matcherType, value );
    }

    public QosRuleMatcher( MatcherType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
