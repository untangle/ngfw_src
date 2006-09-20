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

import org.apache.log4j.Logger;

import org.hibernate.Query;
import org.hibernate.Session;

import com.metavize.mvvm.networking.internal.PPPoESettingsInternal;

import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.MvvmContextFactory;

import com.metavize.mvvm.tran.ValidateException;
import com.metavize.mvvm.util.DataLoader;
import com.metavize.mvvm.util.DataSaver;

class PPPoEManagerImpl
{
    private PPPoESettingsInternal settings;

    private final Logger logger = Logger.getLogger(getClass());
    
    PPPoEManagerImpl()
    {
    }

    /* ----------------- Public  ----------------- */
    
    /* ----------------- Package ----------------- */
    void init()
    {
        try {
            this.settings = loadSettings();
            if ( null == this.settings ) {
                saveSettings( PPPoESettingsInternal.makeInstance( new PPPoESettings()));
            }
        } catch ( ValidateException e ) {
            /* Loaded invalid settings, replacing with bogus replacements */
            logger.warn( "Invalid PPPoESettings in the database, using null", e );
            this.settings = null;
            return;
        }

        /* Possibly register a listener */
    }

    boolean isConnected()
    {
        return false;
    }

    void connect()
    {
        throw new IllegalStateException( "this is not implemented." );
    }

    void disconnect()
    {
        throw new IllegalStateException( "this is not implemented." );
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

