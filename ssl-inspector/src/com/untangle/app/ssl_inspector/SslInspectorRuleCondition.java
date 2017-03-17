/*
 * $Id: SslInspectorRuleCondition.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.ssl_inspector;

import com.untangle.uvm.app.RuleCondition;

@SuppressWarnings("serial")
public class SslInspectorRuleCondition extends RuleCondition
{
    public SslInspectorRuleCondition()
    {
        super();
    }

    public SslInspectorRuleCondition( ConditionType conditionType, String value )
    {
        super(conditionType, value);
    }

    public SslInspectorRuleCondition( ConditionType conditionType, String value, Boolean invert )
    {
        super(conditionType, value, invert);
    }
}
