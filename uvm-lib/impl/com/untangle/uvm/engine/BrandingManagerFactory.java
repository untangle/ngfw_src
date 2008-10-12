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

package com.untangle.uvm.engine;

import java.lang.reflect.Constructor;

import com.untangle.node.util.UtLogger;
import com.untangle.uvm.LocalBrandingManager;
import com.untangle.uvm.RemoteBrandingManager;

class BrandingManagerFactory
{
    private static final String PROPERTY_BRANDING_MANAGER_IMPL = "com.untangle.uvm.branding";
    private static final String PREMIUM_BRANDING_MANAGER_IMPL = "com.untangle.uvm.engine.PremiumBrandingManagerImpl";

    private final UtLogger logger = new UtLogger(getClass());

    /** The stripped down default limited branding manager */
    private final DefaultBrandingManagerImpl limited = new DefaultBrandingManagerImpl();

    /** The branding manager */
    private PremiumBrandingManager premium = null;

    /** remote branding manager */
    private RemoteBrandingManager remote = new RemoteBrandingManagerAdaptor(limited);

    private BrandingManagerFactory() { }

    public LocalBrandingManager getBrandingManager()
    {
        return ( this.premium == null ) ? this.limited : this.premium;
    }

    public RemoteBrandingManager getRemoteBrandingManager()
    {
        return this.remote;
    }

    /* Retest for the premium class */
    public void refresh()
    {
        if ( this.premium != null ) {
            logger.debug( "Already loaded the premium offering" );
            return;
        }

        String className = System.getProperty(PROPERTY_BRANDING_MANAGER_IMPL);
        if ( className == null ) className = PREMIUM_BRANDING_MANAGER_IMPL;

        try {
            Constructor<PremiumBrandingManager> constructor =
                (Constructor<PremiumBrandingManager>)Class.forName( className ).
                getDeclaredConstructor();

            this.premium = constructor.newInstance();
            this.remote = new RemoteBrandingManagerAdaptor(this.premium);
        } catch ( Exception e ) {
            logger.debug( "Could not load premium Branding Manager: " + className);
            this.premium = null;
            this.remote = new RemoteBrandingManagerAdaptor(this.limited);
        }
    }

    public void init()
    {
        /* Premium needs an initialization function */
    }

    public void destroy()
    {
        /* Premium needs an destroy function */
    }

    static BrandingManagerFactory makeInstance()
    {
        BrandingManagerFactory factory = new BrandingManagerFactory();
        factory.refresh();
        return factory;
    }

    /**
     * Inner interface used to indicate the additional methods that the
     * premium offering must implement.
     */
    static interface PremiumBrandingManager extends LocalBrandingManager
    {
    }
}
