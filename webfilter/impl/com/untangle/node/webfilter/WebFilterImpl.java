/*
 * $HeadURL: svn://chef/branch/prod/mawk/work/src/webfilter/impl/com/untangle/node/webfilter/WebFilterImpl.java $
 */
package com.untangle.node.webfilter;

import java.util.LinkedList;

import com.untangle.uvm.node.GenericRule;

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

    public void initializeSettings(WebFilterSettings settings)
    {
        LinkedList<GenericRule> categories = new LinkedList<GenericRule>();

        GenericRule porn = new GenericRule("porn", "Pornography", null, "Pornography", true);
        porn.setBlocked(true);
        porn.setFlagged(true);
        categories.add(porn);
        
        GenericRule proxy = new GenericRule("proxy", "Proxy Sites", null, "Proxy Sites", true);
        porn.setBlocked(true);
        porn.setFlagged(true);
        categories.add(proxy);

        GenericRule rule;
        rule = new GenericRule("aggression", "Hate and Aggression", null, "Hate and Aggression", true);
        categories.add(rule);
        rule = new GenericRule("dating", "Dating", null, "Dating", true);
        categories.add(rule);
        rule = new GenericRule("drugs", "Illegal Drugs", null, "Illegal Drugs", true);
        categories.add(rule);
        rule = new GenericRule("ecommerce", "Shopping", null, "Shopping", true);
        categories.add(rule);
        rule = new GenericRule("gambling", "Gambling", null, "Gambling", true);
        categories.add(rule);
        rule = new GenericRule("hacking", "Hacking", null, "Hacking", true);
        categories.add(rule);
        rule = new GenericRule("jobsearch", "Job Search", null, "Job Search", true);
        categories.add(rule);
        rule = new GenericRule("mail", "Web Mail", null, "Web Mail", true);
        categories.add(rule);
        rule = new GenericRule("porn", "Pornography", null, "Pornography", true);
        categories.add(rule);
        rule = new GenericRule("proxy", "Proxy Sites", null, "Proxy Sites", true);
        categories.add(rule);
        rule = new GenericRule("socialnetworking", "Social Networking", null, "Social Networking", true);
        categories.add(rule);
        rule = new GenericRule("sports", "Sports", null, "Sports", true);
        categories.add(rule);
        rule = new GenericRule("vacation", "Vacation", null, "Vacation", true);
        categories.add(rule);
        rule = new GenericRule("violence", "Violence", null, "Violence", true);
        categories.add(rule);
        rule = new GenericRule("uncategorized", "Uncategorized", null, "Uncategorized", true);
        categories.add(rule);

        settings.setCategories(categories);
    }

}
