/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import com.untangle.uvm.app.RuleCondition;

// THIS IS FOR ECLIPSE - @formatter:off

/**
 * This is a matching criteria for a Capture Control Rule
 * Example: "Destination Port" == "80"
 * Example: "HTTP Host" == "salesforce.com"
 *
 * A CaptureRule has a set of these to determine what traffic to match
 */

// THIS IS FOR ECLIPSE - @formatter:on

@SuppressWarnings("serial")
public class CaptureRuleCondition extends RuleCondition
{
    public CaptureRuleCondition()
    {
        super();
    }

    public CaptureRuleCondition(ConditionType matcherType, String value)
    {
        super(matcherType, value);
    }

    public CaptureRuleCondition(ConditionType matcherType, String value, Boolean invert)
    {
        super(matcherType, value, invert);
    }
}
