/**
 * $Id$
 */
package com.untangle.uvm.network;

import com.untangle.uvm.app.RuleCondition;

/**
 * This is a matching criteria for a Qos Rule
 * Example: "Destination Port" == "80"
 *
 * A QosRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class QosRuleCondition extends RuleCondition
{
    public QosRuleCondition( )
    {
        super();
    }

    public QosRuleCondition( ConditionType matcherType, String value )
    {
        super( matcherType, value );
    }

    public QosRuleCondition( ConditionType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
