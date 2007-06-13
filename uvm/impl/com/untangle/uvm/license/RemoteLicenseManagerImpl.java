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

import java.util.Date;
import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;

public class RemoteLicenseManagerImpl implements RemoteLicenseManager
{
    private final Logger logger = Logger.getLogger(getClass());

    private final LocalLicenseManager licenseManager;

    RemoteLicenseManagerImpl( LocalLicenseManager licenseManager )
    {
        this.licenseManager = licenseManager;
    }
    
    /**
     * Reload all of the available licenses
     */
    public void reloadLicenses()
    {
        if ( this.licenseManager == null ) {
            logger.warn( "The license manager is not available, unable to load licenses" );
            return;
        }

        licenseManager.reloadLicenses();
    }
    
    /**
     * Get the status of a license on a product.
     */
    public LicenseStatus getLicenseStatus( String identifier )
    {
        if ( this.licenseManager == null ) {
            /* no manager, so status is always no license */
            return new LicenseStatus( false, identifier, "unused", new Date( 0 ));
        }

        return this.licenseManager.getLicenseStatus( identifier );
    }    
}
