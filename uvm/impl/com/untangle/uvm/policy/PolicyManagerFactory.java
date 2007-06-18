/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.policy;

import org.apache.log4j.Logger;

public class PolicyManagerFactory
{
    private static final String PROPERTY_POLICY_MANAGER_IMPL = "com.untangle.uvm.policy.premium";
    private static final String PREMIUM_POLICY_MANAGER_IMPL = "com.untangle.uvm.policy.RupPolicyManager";

    private final Logger logger = Logger.getLogger(this.getClass());

    /** The stripped down default limited address book */
    private final DefaultPolicyManager limited = new DefaultPolicyManager();

    /** The premium address book */
    private LocalPolicyManager premium = null;

    /** remote address book */
    private RemotePolicyManager remote = new RemotePolicyManagerAdaptor(limited);

    private PolicyManagerFactory()
    {
    }

    public RemotePolicyManager remotePolicyManager()
    {
        return this.remote;
    }

    public LocalPolicyManager policyManager()
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
            this.premium = (PremiumPolicyManager)Class.forName(className).newInstance();
            this.remote = new RemotePolicyManagerAdaptor(this.premium);
        } catch ( Exception e ) {
            logger.info("Could not load premium PolicyManager: " + className, e);
            this.premium = null;
            this.remote = new RemotePolicyManagerAdaptor(this.limited);
        }
    }

    public static PolicyManagerFactory makeInstance()
    {
        PolicyManagerFactory factory = new PolicyManagerFactory();
        factory.refresh();
        return factory;
    }

    /**
     * Inner interface used to indicate the additional methods that the
     * premium offering must implement.
     */
    static interface PremiumPolicyManager extends LocalPolicyManager
    {
    }
}
