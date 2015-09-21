/*
 * $Id: SslInspectorRuleMatcher.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.node.ssl_inspector;

import com.untangle.uvm.node.RuleMatcher;

@SuppressWarnings("serial")
public class SslInspectorRuleMatcher extends RuleMatcher
{
    public SslInspectorRuleMatcher()
    {
        super();
    }

    public SslInspectorRuleMatcher(MatcherType matcherType, String value)
    {
        super(matcherType, value);
    }

    public SslInspectorRuleMatcher(MatcherType matcherType, String value, Boolean invert)
    {
        super(matcherType, value, invert);
    }
}
