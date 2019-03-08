/**
 * $Id: UpnpRuleCondition.java 41565 201-07-27 22:13:52Z cblaise $
 */
package com.untangle.uvm.network;

import com.untangle.uvm.app.RuleCondition;

/**
 * This is a matching criteria for a Upnp Rule
 * Example: "Destination Port" == "80"
 *
 * A UpnpRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class UpnpRuleCondition extends RuleCondition implements java.io.Serializable, org.json.JSONString
{
    public UpnpRuleCondition( )
    {
        super();
    }

    public UpnpRuleCondition( ConditionType matcherType, String value )
    {
        super( matcherType, value );
    }

    public UpnpRuleCondition( ConditionType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }

    public String toJSONString()
    {
        return (new org.json.JSONObject(this)).toString();
    }
}
