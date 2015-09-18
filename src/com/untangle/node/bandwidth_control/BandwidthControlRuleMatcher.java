/**
 * $Id$
 */
package com.untangle.node.bandwidth_control;

import com.untangle.uvm.node.RuleMatcher;

/**
 * This is a matching criteria for a Bandwidth Control Rule
 */
@SuppressWarnings("serial")
public class BandwidthControlRuleMatcher extends RuleMatcher 
{
    private BandwidthControlApp node;
    
    public BandwidthControlRuleMatcher( )
    {
        super();
    }
    
    public BandwidthControlRuleMatcher( MatcherType matcherType, String value )
    {
        super( matcherType, value );
    }

    public BandwidthControlRuleMatcher( MatcherType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
