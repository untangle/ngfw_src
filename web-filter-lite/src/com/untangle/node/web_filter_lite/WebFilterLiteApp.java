/*
 * $HeadURL$
 */
package com.untangle.node.web_filter_lite;

import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.node.web_filter.DecisionEngine;
import com.untangle.node.web_filter.WebFilterBase;
import com.untangle.node.web_filter.WebFilterSettings;

public class WebFilterLiteApp extends WebFilterBase
{
    private static int web_filter_lite_deployCount = 0;

    protected static final Logger logger = Logger.getLogger(WebFilterLiteApp.class);
    
    /**
     * The WebFilterDecisionEngine is rather big as it loads the database
     * It is lazily initialized in case there are no running Web Filter apps
     */
    private WebFilterLiteDecisionEngine engine = null; 

    public WebFilterLiteApp( NodeSettings nodeSettings, NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );
    }

    @Override
    protected void postStart()
    {
        super.postStart();

        deployWebAppIfRequired();
    }
    
    @Override
    protected void postStop()
    {
        super.postStop();

        unDeployWebAppIfRequired();
    }
    
    @Override
    public synchronized DecisionEngine getDecisionEngine()
    {
        if (engine == null) {
            engine = new WebFilterLiteDecisionEngine(this);
        }
        return engine;
    }

    @Override
    public String getNodeTitle()
    {
        return "Web Filter Lite";
    }

    @Override
    public String getName()
    {
        return "web_filter_lite";
    }

    @Override
    public String getAppName()
    {
        return "web-filter-lite";
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

    private static synchronized void deployWebAppIfRequired()
    {
        web_filter_lite_deployCount = web_filter_lite_deployCount + 1;
        if (web_filter_lite_deployCount != 1) {
            return;
        }

        if ( UvmContextFactory.context().tomcatManager().loadServlet("/web-filter-lite", "web-filter-lite") != null ) {
            logger.debug("Deployed Web_Filter_Lite WebApp");
        } else {
            logger.error("Unable to deploy Web_Filter_Lite WebApp");
        }
    }

    private static synchronized void unDeployWebAppIfRequired()
    {
        web_filter_lite_deployCount = web_filter_lite_deployCount - 1;
        if (web_filter_lite_deployCount != 0) {
            return;
        }

        if (UvmContextFactory.context().tomcatManager().unloadServlet("/web-filter-lite")) {
            logger.debug("Unloaded Web_Filter_Lite WebApp");
        } else {
            logger.warn("Unable to unload Web_Filter_Lite WebApp");
        }
    }
    
}
