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

import java.io.Serializable;

import java.util.Date;

/**
 * Class used to describe the licensing status of an application,
 * presently really only used for the GUI.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class LicenseStatus implements Serializable
{
    /* True if this product ever had a license, false otherwise, used
    * to distinguish products that have gone through a 30-day trial
    * that has expired and products that were never licensed. */
    private final boolean hasLicense;

    /** identifier for this product */
    private final String identifier;
    
    /* name of the type of license, unused if <code>hasLicense</code> is false */
    private final String type;

    /* Date that the license will expire, unused if <code>hasLicense</code> is false */
    private final Date expirationDate;
    
    /* Whether or not the license was expired when this object was created. */
    private final boolean isExpired;
    
    public LicenseStatus( boolean hasLicense, String identifier, String type, Date expirationDate )
    {
        this.hasLicense = hasLicense;
        this.identifier = identifier;
        this.type = type;
        this.expirationDate = expirationDate;
        /* it is unstable if it doesn't have a license or the current time is after the expiration time */
        this.isExpired = !hasLicense || ( System.currentTimeMillis() > expirationDate.getTime());
    }

    public boolean hasLicense()
    {
        return this.hasLicense;
    }

    /** This is the identifier for this product */
    public String getIdentifier()
    {
        return this.identifier;
    }

    public String getType()
    {
        return type;
    }

    public Date getExpirationDate()
    {
        return this.expirationDate;
    }
    
    /* Done this way so that timezone issues between the GUI and the UVM */
    public boolean isExpired()
    {
        return this.isExpired;
    }

}

