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

    private LicenseManager realLicenseManager = null;
    private LicenseManager nilLicenseManager = null;

    private final String LICENSE_CLASS_NAME = "com.untangle.uvm.license.LicenseManagerImpl";

    private LicenseManagerFactory() {} //prevent instantiation

    public LicenseManager getLicenseManager()
    {
        if ( this.nilLicenseManager == null || this.realLicenseManager == null) {
            refresh();
        }
        
        if ( this.realLicenseManager != null )
            return this.realLicenseManager;
        else 
            return this.nilLicenseManager;
    }
        
    public static LicenseManagerFactory makeInstance()
    {
        LicenseManagerFactory factory = new LicenseManagerFactory();
        factory.refresh();
        return factory;
    }

    private void refresh()
    {
        // load the Nil Manager
        if (this.nilLicenseManager == null) 
            this.nilLicenseManager = new NilLicenseManagerImpl( );
        
        // load the Real Manager
        try {
            loadRealLicenseManager();
        } catch ( Exception e ) {
            logger.warn( "Unable to load the license manager", e );
        }
    }

    @SuppressWarnings("unchecked") //Class.forName
    private synchronized void loadRealLicenseManager()
    {
        /* if already loaded just return */
        if ( this.realLicenseManager != null ) 
            return;

        try {
            logger.warn("Loading License Manager...\n");

            Class<LicenseManager> clz = (Class<LicenseManager>)Class.forName( LICENSE_CLASS_NAME );
            this.realLicenseManager = (LicenseManager)(clz.getMethod( "getInstance" ).invoke( null ));

            logger.warn("Loading License Manager... done\n");

        } catch ( java.lang.ClassNotFoundException e ) {
            //this happens if the license node isn't on the server
            this.realLicenseManager = null;
        } catch ( Exception e ) {
            this.realLicenseManager = null;
            logger.info("could not load LicenseManager: " + LICENSE_CLASS_NAME, e );
            throw new RuntimeException( "Unable to load the license manager", e );
        }

    }
}
