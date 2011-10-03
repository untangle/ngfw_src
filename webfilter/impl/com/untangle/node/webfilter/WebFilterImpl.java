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
    private WebFilterDecisionEngine engine = null; 
    
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


        GenericRule rule;
        rule = new GenericRule("aggression", "Hate and Aggression", null, "Hate and Aggression sites ", true, false, false);
        categories.add(rule);
        rule = new GenericRule("dating", "Dating", null, "Dating sites", true, false, false);
        categories.add(rule);
        rule = new GenericRule("drugs", "Illegal Drugs", null, "Illegal Drugs sites", true, false, false);
        categories.add(rule);
        rule = new GenericRule("ecommerce", "Shopping", null, "Shopping and eCommerce sites", true, false, false);
        categories.add(rule);
        rule = new GenericRule("gambling", "Gambling", null, "Gambling sites", true, false, false);
        categories.add(rule);
        rule = new GenericRule("hacking", "Hacking", null, "Hacking sites", true, false, false);
        categories.add(rule);
        rule = new GenericRule("jobsearch", "Job Search", null, "Job Search sites", true, false, false);
        categories.add(rule);
        rule = new GenericRule("mail", "Web Mail", null, "Web Mail sites", true, false, false);
        categories.add(rule);
        rule = new GenericRule("porn", "Pornography", null, "Pornography sites", true, true, true);
        categories.add(rule);
        rule = new GenericRule("proxy", "Proxy Sites", null, "Proxy/Anonymizer sites", true, true, true);
        categories.add(rule);
        rule = new GenericRule("socialnetworking", "Social Networking", null, "Social Networking sites", true, false, false);
        categories.add(rule);
        rule = new GenericRule("sports", "Sports", null, "Sports sites", true, false, false);
        categories.add(rule);
        rule = new GenericRule("vacation", "Vacation", null, "Vacation sites", true, false, false);
        categories.add(rule);
        rule = new GenericRule("violence", "Violence", null, "Violence sites", true, false, false);
        categories.add(rule);
        rule = new GenericRule("uncategorized", "Uncategorized", null, "Uncategorized sites", true, false, false);
        categories.add(rule);

        settings.setCategories(categories);
    }

}
