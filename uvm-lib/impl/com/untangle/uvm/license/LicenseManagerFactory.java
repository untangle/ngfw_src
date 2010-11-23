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


public class LicenseManagerFactory
{
    private final Logger logger = Logger.getLogger(getClass());

    private LicenseManager licenseManager = null;

    private LicenseManagerFactory()
    {
    }

    public LicenseManager getLicenseManager()
    {
        if ( this.licenseManager == null )
            refresh();

        return licenseManager;
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
        } catch ( Exception e ) {
            logger.warn( "Unable to load the license manager", e );
        }
    }

    @SuppressWarnings("unchecked") //Class.forName
    private synchronized void loadLicenseManager()
    {
        /* if already loaded just return */
        if ( this.licenseManager != null && this.licenseManager.getClass().equals("com.untangle.uvm.license.LicenseManagerImpl") )
            return;

        String className = "com.untangle.uvm.license.LicenseManagerImpl";
        try {
            Class<LicenseManager> clz = (Class<LicenseManager>)Class.forName( className );
            
            this.licenseManager = (LicenseManager)(clz.getMethod( "getInstance" ).invoke( null ));
        } catch ( java.lang.ClassNotFoundException e ) {
            //this happens if the license node isn't on the server
            this.licenseManager = null;
        } catch ( Exception e ) {
            logger.info("could not load LicenseManager: " + className, e );
            
            this.licenseManager = null;
            throw new RuntimeException( "Unable to load the license manager", e );
        }
        
        if (this.licenseManager == null) {
            // if we failed to load the real thing, just load the empty nil license manager
            this.licenseManager = new NilLicenseManagerImpl( );
        }
    }
}
