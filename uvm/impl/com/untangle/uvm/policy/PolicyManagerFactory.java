/* $HeadURL$ */

package com.untangle.uvm.policy;

import org.apache.log4j.Logger;

public class PolicyManagerFactory
{
    private static final String PROPERTY_POLICY_MANAGER_IMPL = "com.untangle.uvm.policy.premium";
    private static final String PREMIUM_POLICY_MANAGER_IMPL = "com.untangle.uvm.policy.PolicyManagerImpl";

    private final Logger logger = Logger.getLogger(this.getClass());

    /** The stripped down default limited address book */
    private final DefaultPolicyManager limited = new DefaultPolicyManager();

    /** The premium address book */
    private PolicyManager premium = null;

    private PolicyManagerFactory()
    {
    }

    public PolicyManager policyManager()
    {
        return ( this.premium == null ) ? this.limited : this.premium;
    }

    /* Retest for the premium class */
    public void refresh()
    {
        if ( this.premium != null ) {
            logger.debug( "Already loaded the premium offering" );
            return;
        }

        String className = System.getProperty(PROPERTY_POLICY_MANAGER_IMPL);
        if (null == className) {
            className = PREMIUM_POLICY_MANAGER_IMPL;
        }
        try {
            this.premium = (PolicyManager)Class.forName(className).newInstance();
        } catch ( Exception e ) {
            logger.debug("Could not load premium PolicyManager: " + className);
            this.premium = null;
        }
    }

    public static PolicyManagerFactory makeInstance()
    {
        PolicyManagerFactory factory = new PolicyManagerFactory();
        factory.refresh();
        return factory;
    }
}
