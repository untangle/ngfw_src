/**
 * $Id$
 */
package com.untangle.uvm.network;

import com.untangle.uvm.app.RuleCondition;


/**
 * This is a matching criteria for a PortForward Rule
 * Example: "Destination Port" == "80"
 *
 * A PortForwardRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class PortForwardRuleCondition extends RuleCondition implements java.io.Serializable, org.json.JSONString
{
    public PortForwardRuleCondition( )
    {
        super();
    }

    public PortForwardRuleCondition( ConditionType matcherType, String value )
    {
        super( matcherType, value );
    }

    public PortForwardRuleCondition( ConditionType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }

    public String toJSONString()
    {
        return (new org.json.JSONObject(this)).toString();
    }
}
