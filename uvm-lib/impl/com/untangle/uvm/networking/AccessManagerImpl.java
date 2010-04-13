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

package com.untangle.uvm.networking;

import static com.untangle.uvm.networking.ShellFlags.FLAG_BLOCK_PAGE_PORT;
import static com.untangle.uvm.networking.ShellFlags.FLAG_HTTPS_OUT;
import static com.untangle.uvm.networking.ShellFlags.FLAG_HTTPS_RES;
import static com.untangle.uvm.networking.ShellFlags.FLAG_HTTP_IN;
import static com.untangle.uvm.networking.ShellFlags.FLAG_OUT_MASK;
import static com.untangle.uvm.networking.ShellFlags.FLAG_OUT_NET;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.networking.internal.AccessSettingsInternal;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.script.ScriptWriter;
import com.untangle.uvm.toolbox.UpstreamManager;
import com.untangle.uvm.util.DataLoader;
import com.untangle.uvm.util.DataSaver;
import com.untangle.uvm.util.DeletingDataSaver;

class AccessManagerImpl implements LocalAccessManager
{
    
    /* These are the services keys, each one must be unique */
    private static final String KEY_ADMINISTRATION = "administration";
    private static final String KEY_QUARANTINE = "quarantine";
    private static final String KEY_REPORTING = "reporting";

    private final Logger logger = Logger.getLogger(getClass());

    private AccessSettingsInternal accessSettings = null;

    private final Set<String> servicesSet = new HashSet<String>();

    AccessManagerImpl()
    {
    }

    /* Use this to retrieve just the remote settings */
    public AccessSettings getSettings()
    {
        return this.accessSettings.toSettings();
    }

    public AccessSettingsInternal getInternalSettings()
    {
        return this.accessSettings;
    }

    /* Use this to mess with the remote settings without modifying the network settings */
    public synchronized void setSettings( AccessSettings settings )
    {
        setSettings( settings, false );
    }

    /* Use this to mess with the remote settings without modifying the network settings */
    public synchronized void setSettings( AccessSettings settings, boolean forceSave )
    {
        /* should validate settings */
        if ( !forceSave && settings.isClean()) logger.debug( "settings are clean, leaving alone." );
        
        /* Need to save the settings to the database, then update the
         * local value, everything is executed later */
        DataSaver<AccessSettings> saver = 
            new DeletingDataSaver<AccessSettings>( LocalUvmContextFactory.context(), "AccessSettings" );

        AccessSettingsInternal newSettings = AccessSettingsInternal.makeInstance( settings );
        saver.saveData( newSettings.toSettings());
        this.accessSettings = newSettings;

        setSupportAccess( this.accessSettings );
    }

    /* ---------------------- PACKAGE ---------------------- */
    /* Initialize the settings, load at startup */
    synchronized void init()
    {
        DataLoader<AccessSettings> loader =
            new DataLoader<AccessSettings>( "AccessSettings", LocalUvmContextFactory.context());

        AccessSettings settings = loader.loadData();
        
        if ( settings == null ) {
            logger.info( "There are no access settings in the database, must initialize from files." );
            
            setSettings( NetworkConfigurationLoader.getInstance().loadAccessSettings(), true );
        } else {
            // Need to ignore now obsolete db setting of support flag
            NetworkConfigurationLoader.getInstance().loadSupportFlag(settings);
            this.accessSettings = AccessSettingsInternal.makeInstance( settings );
        }
    }
    
    /* Invoked at the very end to actually update the settings */
    /* This should also be synchronized from its callers. */
    synchronized void commit( ScriptWriter scriptWriter )
    {
	if ( this.accessSettings == null ) {
	    logger.warn( "Null access settings" );
	    return;
	}
        setServiceStatus( this.accessSettings.getIsOutsideAdministrationEnabled(), KEY_ADMINISTRATION );
        setServiceStatus( this.accessSettings.getIsOutsideQuarantineEnabled(), KEY_QUARANTINE );
        setServiceStatus( this.accessSettings.getIsOutsideReportingEnabled(), KEY_REPORTING );
        
        /* Save the variables that need to be written to networking.sh */
        updateShellScript( scriptWriter, this.accessSettings );
    }


    /** Register a service, used to determine if port 443 should be open or not */
    synchronized void registerService( String name )
    {
        this.servicesSet.add( name );
    }

    /** Register a service, used to determine if port 443 should be open or not */
    synchronized void unregisterService( String name )
    {
        this.servicesSet.remove( name );
    }

    /* ---------------------- PRIVATE ---------------------- */
    private void updateShellScript( ScriptWriter scriptWriter, AccessSettingsInternal access )
    {
        if ( access == null ) {
            logger.warn( "unable to save hostname, access settings are not initialized." );            
            return;
        }

        scriptWriter.appendVariable( FLAG_HTTP_IN, access.getIsInsideInsecureEnabled());

        // access.getIsOutsideAccessEnabled() is no longer used, HTTPs
        // is automatically opened if there are any services that need
        // it.
        scriptWriter.appendVariable( FLAG_HTTPS_OUT, !this.servicesSet.isEmpty());
        scriptWriter.appendVariable( FLAG_HTTPS_RES, access.getIsOutsideAccessRestricted());

        IPaddr outsideNetwork = access.getOutsideNetwork();
        IPaddr outsideNetmask = access.getOutsideNetmask();
        
        if (( outsideNetwork != null ) && !outsideNetwork.isEmpty()) {
            scriptWriter.appendVariable( FLAG_OUT_NET, outsideNetwork.toString());
            
            if (( outsideNetmask != null ) && !outsideNetmask.isEmpty()) {
                scriptWriter.appendVariable( FLAG_OUT_MASK, outsideNetmask.toString());
            }
        }

        scriptWriter.appendVariable( FLAG_BLOCK_PAGE_PORT, access.getBlockPagePort());
    }

    private void setSupportAccess( AccessSettingsInternal access )
    {
        if ( !NetworkManagerImpl.getInstance().getSaveSettings()) {
            logger.warn( "not modifying support access as requested." );
            return;
        }

        if ( access == null ) {
            logger.warn( "unable to set support access, address settings are not initialized." );            
            return;
        }
        
        try {
            if ( access.getIsSupportEnabled()) {
                LocalUvmContextFactory.context().upstreamManager().enableService(UpstreamManager.SUPPORT_SERVICE_NAME);
            } else {
                LocalUvmContextFactory.context().upstreamManager().disableService(UpstreamManager.SUPPORT_SERVICE_NAME);
            }
        } catch ( Exception ex ) {
            logger.error( "Unable to enable support", ex );
        }
    }

    private void setServiceStatus( boolean status, String key )
    {
        if ( status ) registerService( key );
        else          unregisterService( key );
    }
}

