/*
 * $HeadURL: svn://chef/branch/prod/mawk/work/src/webfilter/impl/com/untangle/node/webfilter/WebFilterImpl.java $
 */
package com.untangle.node.webfilter;

import java.util.LinkedList;

import com.untangle.uvm.node.GenericRule;

public class WebFilterImpl extends WebFilterBase
{
    /**
     * The WebFilterDecisionEngine is rather big as it loads the database
     * It is lazily initialized in case there are no running Web Filter apps
     */
    private static WebFilterDecisionEngine engine = null; 
    
    @Override
    public synchronized DecisionEngine getDecisionEngine()
    {
        if (engine == null) {
            engine = new WebFilterDecisionEngine(this);
        }
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

    @Override
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
        rule = new GenericRule("aggression", "Hate and Aggression", null, "Hate and Aggression", true, false, false);
        categories.add(rule);
        rule = new GenericRule("dating", "Dating", null, "Dating", true, false, false);
        categories.add(rule);
        rule = new GenericRule("drugs", "Illegal Drugs", null, "Illegal Drugs", true, false, false);
        categories.add(rule);
        rule = new GenericRule("ecommerce", "Shopping", null, "Shopping", true, false, false);
        categories.add(rule);
        rule = new GenericRule("gambling", "Gambling", null, "Gambling", true, false, false);
        categories.add(rule);
        rule = new GenericRule("hacking", "Hacking", null, "Hacking", true, false, false);
        categories.add(rule);
        rule = new GenericRule("jobsearch", "Job Search", null, "Job Search", true, false, false);
        categories.add(rule);
        rule = new GenericRule("mail", "Web Mail", null, "Web Mail", true, false, false);
        categories.add(rule);
        rule = new GenericRule("porn", "Pornography", null, "Pornography", true, true, true);
        categories.add(rule);
        rule = new GenericRule("proxy", "Proxy Sites", null, "Proxy Sites", true, true, true);
        categories.add(rule);
        rule = new GenericRule("socialnetworking", "Social Networking", null, "Social Networking", true, false, false);
        categories.add(rule);
        rule = new GenericRule("sports", "Sports", null, "Sports", true, false, false);
        categories.add(rule);
        rule = new GenericRule("vacation", "Vacation", null, "Vacation", true, false, false);
        categories.add(rule);
        rule = new GenericRule("violence", "Violence", null, "Violence", true, false, false);
        categories.add(rule);
        rule = new GenericRule("uncategorized", "Uncategorized", null, "Uncategorized", true, false, false);
        categories.add(rule);

        settings.setCategories(categories);
    }

}
