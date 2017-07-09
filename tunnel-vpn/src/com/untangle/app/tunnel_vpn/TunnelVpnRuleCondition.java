/**
 * $Id$
 */
package com.untangle.app.tunnel_vpn;

import com.untangle.uvm.app.RuleCondition;

/**
 * This is a matching criteria for a TunnelVpnRule
 * Example: "Destination Port" == "80"
 *
 * A TunnelVpnRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class TunnelVpnRuleCondition extends RuleCondition
{
    public TunnelVpnRuleCondition( )
    {
        super();
    }

    public TunnelVpnRuleCondition( ConditionType matcherType, String value )
    {
        super( matcherType, value );
    }

    public TunnelVpnRuleCondition( ConditionType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
