/**
 * $Id: CaptureRuleMatcher.java,v 1.00 2011/08/24 14:43:42 dmorris Exp $
 */

package com.untangle.node.capture;

import com.untangle.uvm.node.RuleMatcher;

/**
 * This is a matching criteria for a Capture Control Rule
 * Example: "Destination Port" == "80"
 * Example: "HTTP Host" == "salesforce.com"
 *
 * A CaptureRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class CaptureRuleMatcher extends RuleMatcher
{
    public CaptureRuleMatcher( )
    {
        super();
    }

    public CaptureRuleMatcher( MatcherType matcherType, String value )
    {
        super( matcherType, value );
    }

    public CaptureRuleMatcher( MatcherType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
