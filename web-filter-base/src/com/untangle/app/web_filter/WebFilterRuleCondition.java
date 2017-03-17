/**
 * $Id: WebFilterRuleCondition.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.web_filter;

import com.untangle.uvm.app.RuleCondition;

/**
 * This is a matching criteria for a WebFilterRuleCondition Example:
 * "Destination Port" == "80" Example: "HTTP Host" == "salesforce.com"
 * 
 * A WebFilterLogicRule has a set of these to determine what traffic to match
 */

@SuppressWarnings("serial")
public class WebFilterRuleCondition extends RuleCondition
{
    public WebFilterRuleCondition()
    {
        super();
    }

    public WebFilterRuleCondition( ConditionType conditionType, String value)
    {
        super(conditionType, value);
    }

    public WebFilterRuleCondition(ConditionType conditionType, String value, Boolean invert)
    {
        super(conditionType, value, invert);
    }
}
