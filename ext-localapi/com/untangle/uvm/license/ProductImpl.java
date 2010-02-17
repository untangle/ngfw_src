/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.uvm.license;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;

public class ProductImpl implements LicenseManager.Product
{
    private final Logger logger = Logger.getLogger( getClass());
    private final String identifier;

    /* this will automatically retry to register if the license
     * manager wasn't available */
    private boolean isRegistered = false;

    /* Activated until deactivated */
    private boolean isActivated = true;

    private long expirationTimeMillis;

    public ProductImpl( String identifier )
    {
        this.identifier = identifier;
    }

    public final String identifier()
    {
        return this.identifier;
    }

    public final boolean isExpired()
    {
        try {
            if ( !this.isRegistered ) {
                synchronized ( this ) {
                    /* Check again after getting the lock */
                    if (!this.isRegistered ) {
                        LicenseManager licenseManager =
                            (LicenseManager)LocalUvmContextFactory.context().localLicenseManager();
                        licenseManager.register( this );

                        this.isRegistered = true;
                    }
                }
            }
        } catch ( Exception e ) {
            logger.warn( "Unable to register the " + this.identifier +  " the license manager", e );
            /* Unable to register, this is expired */
            return true;
        }

        return ( System.nanoTime() / 1000000l ) > getExpirationTimeMillis();
    }

    public final long getExpirationTimeMillis()
    {
        return this.expirationTimeMillis;
    }

    public final void updateExpirationDate( long end )
    {
        /* only call the reactivation event if it isn't activated,
         * make sure to call this BEFORE updating the expiration date,
         * this way the product can prepare before isExpired becomes
         * true. */
        if ( !this.isActivated ) {
            try {
                logger.debug( "The product " + identifier + " is being reativated." );
                reactivationEvent();
            } catch ( Exception e ) {
                logger.warn( "error during reactivation event.", e );
            }

            this.isActivated = true;
        }

        this.expirationTimeMillis = end;
    }

    /* The default one doesn't do anything */
    public final void expire()
    {
        logger.debug( "The product " + identifier + " has expired [" + this.isActivated + "]." );
        try {
            if ( this.isActivated ) expireEvent();
        } catch ( Exception e ) {
            logger.warn( "error during expire event.", e );
        }

        this.isActivated = false;

        /* call this after so all subsequent calls to isExpired
         * failed, this way the expire event has a chance to do its
         * thing first */
        this.expirationTimeMillis =( System.nanoTime() / 1000000l )  - 1000;
    }

    /** This is the event that is sent to the product, this should be
     * overriden if you want to do something interesting on expire */
    protected void expireEvent()
    {
        logger.debug( "default expire event" );
    }

    /* Override this method if you have a hook for when a product is
     * reactivated */
    protected void reactivationEvent()
    {
        logger.debug( "The product " + identifier + " has been reactivated." );
    }

    public boolean isActivated()
    {
        return this.isActivated;
    }
}
