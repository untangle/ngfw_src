/**
 * $Id: ApplicationControlLogicRuleCondition.java 37269 2014-02-26 23:46:16Z dmorris $
 */
package com.untangle.app.application_control;

import com.untangle.uvm.app.RuleCondition;

/**
 * This is a matching criteria for an ApplicationControlLogicRuleCondition
 * Example: "Destination Port" == "80" Example: "HTTP Host" == "salesforce.com"
 * 
 * A ApplicationControlLogicRule has a set of these to determine what traffic to
 * match
 */

@SuppressWarnings("serial")
public class ApplicationControlLogicRuleCondition extends RuleCondition
{
    /**
     * Constructor
     */
    public ApplicationControlLogicRuleCondition()
    {
        super();
    }

    /**
     * Constructor
     * 
     * @param conditionType
     *        The condition type
     * @param value
     *        The value
     */
    public ApplicationControlLogicRuleCondition(ConditionType conditionType, String value)
    {
        super(conditionType, value);
    }

    /**
     * Constructor
     * 
     * @param conditionType
     *        The condition type
     * @param value
     *        The value
     * @param invert
     *        Invert flag
     */
    public ApplicationControlLogicRuleCondition(ConditionType conditionType, String value, Boolean invert)
    {
        super(conditionType, value, invert);
    }
}
