/*
 * Copyright (c) 2003, 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.networking;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import org.hibernate.Query;
import org.hibernate.Session;

import com.metavize.mvvm.ArgonException;
import com.metavize.mvvm.IntfConstants;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.MvvmLocalContext;

import com.metavize.mvvm.localapi.ArgonInterface;
import com.metavize.mvvm.localapi.LocalIntfManager;

import com.metavize.mvvm.networking.internal.PPPoEConnectionInternal;
import com.metavize.mvvm.networking.internal.PPPoESettingsInternal;

import com.metavize.mvvm.tran.ValidateException;
import com.metavize.mvvm.util.DataLoader;
import com.metavize.mvvm.util.DataSaver;

class PPPoEManagerImpl
{
    /* The current settings */
    private PPPoESettingsInternal settings;
    
    /* The list of interfaces that have presently been registered as a PPP interface */
    private List<ArgonInterface> registeredIntfList = new LinkedList<ArgonInterface>();

    /* The list of interfaces set at startup */
    private List<ArgonInterface> startupIntfList = new LinkedList<ArgonInterface>();

    private final Logger logger = Logger.getLogger(getClass());
    
    PPPoEManagerImpl()
    {
    }

    /* ----------------- Public  ----------------- */
    public PPPoESettingsInternal getInternalSettings()
    {
        return this.settings;
    }

    public PPPoESettings getSettings()
    {
        if ( this.settings == null ) {
            logger.warn( "null PPPoE Settings, returning new settings object" );
            return new PPPoESettings();
        }
        return this.settings.toSettings();
    }

    public void setSettings( PPPoESettings newSettings ) throws PPPoEException
    {
        try {
            saveSettings( PPPoESettingsInternal.makeInstance( newSettings ));
        } catch ( ValidateException e ) {
            throw new PPPoEException( "Error saving settings", e );
        }
    }
        
    /* ----------------- Package ----------------- */
    void init()
    {
        try {
            this.settings = loadSettings();
            if ( null == this.settings ) saveDefaults();

            /* Save the list of interfaces from startup */
            startupIntfList = MvvmContextFactory.context().intfManager().getIntfList();

        } catch ( ValidateException e ) {
            /* Loaded invalid settings, replacing with bogus replacements */
            logger.warn( "Invalid PPPoESettings in the database, using null", e );
            this.settings = null;
        }

        /* Possibly register a listener */
        try {
            registerIntfs();
        } catch ( PPPoEException e ) {
            logger.warn( "Unable to register ppp interfaces at startup, disabling" );
            resetIntfs();
        }
    }

    void isConnected()
    {
    }

    /* This re-registers all of the interfaces as PPP devices */
    synchronized void registerIntfs() throws PPPoEException
    {
        /* Unregister all of the active interfaces */
        unregisterIntfs();

        /* Don't need to do anything if the connection is not enabled */
        if (( null == this.settings ) || !this.settings.getIsEnabled()) return;

        LocalIntfManager lim = MvvmContextFactory.context().intfManager();
        
        List<ArgonInterface> registeredIntfList = new LinkedList<ArgonInterface>();
        
        for ( PPPoEConnectionInternal connection : this.settings.getConnectionList()) {
            /* Skip all of the connections that are not active */
            if ( !connection.isLive()) continue;
            
            /* Retrieve the current interface mapping */
            try {
                ArgonInterface intf = lim.getIntfByArgon( connection.getArgonIntf());
                registeredIntfList.add( intf );
            } catch ( ArgonException e ) {
                logger.warn( "Unable to lookup an argon interface for: " + connection, e );
                continue;
            }

            /* Register the device */
            try {
                lim.registerSecondaryIntf( connection.getDeviceName(), connection.getArgonIntf());
            } catch ( ArgonException e ) {
                logger.warn( "Unable to register the interface for: " + connection, e );
                /* Set the registered interface list before throwing the exception so
                 * that unregister will replace all of the correct interfaces */
                this.registeredIntfList = registeredIntfList;
                throw new PPPoEException( "Unable to register the interface for: " + connection, e );
            }
        }
        
        this.registeredIntfList = registeredIntfList;
    }

    synchronized void unregisterIntfs()
    {
        if ( null == this.registeredIntfList ) {
            logger.info( "there are no registered interfaces" );
            return;
        }

        LocalIntfManager lim = MvvmContextFactory.context().intfManager();
        
        /* Iterate each of the cached entries and replace with their original values */
        for ( ArgonInterface intf : this.registeredIntfList ) {
            try {
                lim.unregisterSecondaryIntf( intf.getArgon());
            } catch ( ArgonException e ) {
                logger.warn( "Unable to register the interface for: " + intf + " continuing.", e );
                resetIntfs();
            }
        }

        /* Set to null to avoid unregistering twice */
        this.registeredIntfList = null;
    }

    /* This is for the ohh no situtation, just a way to get back to the interfaces at startup */
    synchronized void resetIntfs()
    {
        MvvmContextFactory.context().intfManager().resetSecondaryIntfs();
        
        this.registeredIntfList = null;
    }

    /* ----------------- Private ----------------- */
    /* Load the settings from the database */
    private PPPoESettingsInternal loadSettings() throws ValidateException
    {
        DataLoader<PPPoESettings> loader = new DataLoader<PPPoESettings>( "PPPoESettings",
                                                                          MvvmContextFactory.context());
        PPPoESettings dbSettings = loader.loadData();

        /* No database settings */
        if ( dbSettings == null ) {
            logger.info( "There are no network database settings" );
            return null;
        }

        return PPPoESettingsInternal.makeInstance( dbSettings );
    }

    /* Save the settings to the database */
    private void saveSettings( PPPoESettingsInternal newSettings )
    {
        DataSaver<PPPoESettings> saver =
            new PPPoESettingsDataSaver( MvvmContextFactory.context());

        if ( saver.saveData( newSettings.toSettings()) == null ) {
            logger.error( "Unable to save the pppoe settings." );
            return;
        }

        this.settings = newSettings;
    }

    /* Save the initial default settings */
    private void saveDefaults() throws ValidateException
    {
        PPPoESettings settings = new PPPoESettings();
        settings.setIsEnabled( true );
        saveSettings( PPPoESettingsInternal.makeInstance( settings ));
    }

    /* Data saver used to delete other instances of the object */
    private static class PPPoESettingsDataSaver extends DataSaver<PPPoESettings>
    {
        PPPoESettingsDataSaver( MvvmLocalContext local )
        {
            super( local );
        }

        @Override
        protected void preSave( Session s )
        {
            Query q = s.createQuery( "from PPPoESettings" );
            for ( Iterator iter = q.iterate() ; iter.hasNext() ; ) {
                PPPoESettings settings = (PPPoESettings)iter.next();
                s.delete( settings );
            }
        }
    }
}

