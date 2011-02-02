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
    private static final String PREMIUM_POLICY_MANAGER_IMPL = "com.untangle.uvm.policy.PolicyManagerImpl";

    private final Logger logger = Logger.getLogger(this.getClass());

    /** The stripped down default limited address book */
    private final DefaultPolicyManager limited = new DefaultPolicyManager();

    /** The premium address book */
    private PolicyManager premium = null;

    /** remote policy manager */
    private RemotePolicyManager remote = new RemotePolicyManagerAdaptor(limited);

    private PolicyManagerFactory()
    {
    }

    public RemotePolicyManager remotePolicyManager()
    {
        return this.remote;
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
            this.remote = new RemotePolicyManagerAdaptor(this.premium);
        } catch ( Exception e ) {
            logger.debug("Could not load premium PolicyManager: " + className);
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
}
