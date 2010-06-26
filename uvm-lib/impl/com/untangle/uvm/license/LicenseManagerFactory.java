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
package com.untangle.uvm.license;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmException;

public class LicenseManagerFactory
{
    private final Logger logger = Logger.getLogger(getClass());

    private LocalLicenseManager localManager;

    private RemoteLicenseManagerImpl remoteManager = new RemoteLicenseManagerImpl( null );

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

    @SuppressWarnings("unchecked") //Class.forName
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
