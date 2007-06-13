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

import com.untangle.uvm.UvmException;

public class LicenseManagerFactory
{
    private final Logger logger = Logger.getLogger(getClass());

    private LocalLicenseManager localManager;

    private RemoteLicenseManager remoteManager = new RemoteLicenseManagerImpl( null );

    private LicenseManagerFactory()
    {
    }

    public LocalLicenseManager getLocalLicenseManager() throws UvmException
    {
        if ( this.localManager == null ) loadLicenseManager();

        return localManager;
    }

    public RemoteLicenseManager getRemoteLicenseManager()
    {
        /* Refresh, doesn't matter if there is a failure, it just wants the remote manager */
        refresh();
        
        return this.remoteManager;
    }
        
    public static LicenseManagerFactory makeInstance()
    {
        LicenseManagerFactory factory = new LicenseManagerFactory();
        factory.refresh();
        return factory;
    }

    private void refresh()
    {
        try {
            loadLicenseManager();
        } catch ( UvmException e ) {
            logger.info( "Unable to load the license manager", e );
        }
    }

    private synchronized void loadLicenseManager() throws UvmException
    {
        /* already loaded, this is why it is synchronized */
        if ( this.localManager != null ) return;

        String className = "com.untangle.uvm.license.LicenseManagerImpl";
        try {
            Class<LocalLicenseManager> clz = (Class<LocalLicenseManager>)Class.forName( className );
            
            this.localManager = (LocalLicenseManager)(clz.getMethod( "getInstance" ).invoke( null ));
        } catch ( Exception e ) {
            logger.info("could not load LicenseManager: " + className, e );
            
            this.localManager = null;
            throw new UvmException( "Unable to load the license manager", e );
        }
        
        /* Create a new remote license manager */
        this.remoteManager = new RemoteLicenseManagerImpl( this.localManager );
    }

}
