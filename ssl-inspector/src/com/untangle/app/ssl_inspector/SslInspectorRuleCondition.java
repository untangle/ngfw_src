/**
 * $Id: SslInspectorRuleCondition.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.ssl_inspector;

import com.untangle.uvm.app.RuleCondition;

/**
 * This is a matching criteria for an SslInspectorRule
 *  
 * Examples: "Destination Port" == "80"
 * Example: "HTTP Host" == "salesforce.com"
 * 
 * An SslInspectorRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class SslInspectorRuleCondition extends RuleCondition
{
    /**
     * Constructor
     */
    public SslInspectorRuleCondition()
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
    public SslInspectorRuleCondition(ConditionType conditionType, String value)
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
    public SslInspectorRuleCondition(ConditionType conditionType, String value, Boolean invert)
    {
        super(conditionType, value, invert);
    }
}
