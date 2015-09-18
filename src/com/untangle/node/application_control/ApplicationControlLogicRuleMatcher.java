/**
 * $Id: ApplicationControlLogicRuleMatcher.java 37269 2014-02-26 23:46:16Z dmorris $
 */
package com.untangle.node.application_control;

import com.untangle.uvm.node.RuleMatcher;

/**
 * This is a matching criteria for an ApplicationControlLogicRuleMatcher Example:
 * "Destination Port" == "80" Example: "HTTP Host" == "salesforce.com"
 * 
 * A ApplicationControlLogicRule has a set of these to determine what traffic to match
 */

@SuppressWarnings("serial")
public class ApplicationControlLogicRuleMatcher extends RuleMatcher
{
    public ApplicationControlLogicRuleMatcher()
    {
        super();
    }

    public ApplicationControlLogicRuleMatcher(MatcherType matcherType, String value)
    {
        super(matcherType, value);
    }

    public ApplicationControlLogicRuleMatcher(MatcherType matcherType, String value, Boolean invert)
    {
        super(matcherType, value, invert);
    }
}
