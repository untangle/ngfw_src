/*
 * $HeadURL: svn://chef/branch/prod/mawk/work/src/webfilter/impl/com/untangle/node/webfilter/WebFilterImpl.java $
 */
package com.untangle.node.webfilter;

public class WebFilterImpl extends WebFilterBase
{
    private final WebFilterDecisionEngine engine = new WebFilterDecisionEngine(this);

    @Override
    public DecisionEngine getDecisionEngine()
    {
        return engine;
    }

    @Override
    public String getVendor()
    {
        return "untangle";
    }

    @Override
    public String getNodeTitle()
    {
        return "Web Filter Lite";
    }

    @Override
    public String getName()
    {
        return "webfilter";
    }
}
