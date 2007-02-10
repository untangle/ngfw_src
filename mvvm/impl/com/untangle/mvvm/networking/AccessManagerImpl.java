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

package com.untangle.mvvm.networking;

import org.apache.log4j.Logger;

import com.untangle.mvvm.MvvmContextFactory;

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.script.ScriptRunner;
import com.untangle.mvvm.tran.script.ScriptWriter;

import com.untangle.mvvm.util.DataLoader;
import com.untangle.mvvm.util.DataSaver;
import com.untangle.mvvm.util.DeletingDataSaver;

import com.untangle.mvvm.networking.internal.AccessSettingsInternal;

import static com.untangle.mvvm.networking.NetworkManagerImpl.BUNNICULA_BASE;

import static com.untangle.mvvm.networking.ShellFlags.FLAG_HTTP_IN;
import static com.untangle.mvvm.networking.ShellFlags.FLAG_HTTPS_OUT;
import static com.untangle.mvvm.networking.ShellFlags.FLAG_HTTPS_RES;
import static com.untangle.mvvm.networking.ShellFlags.FLAG_OUT_NET;
import static com.untangle.mvvm.networking.ShellFlags.FLAG_OUT_MASK;

class AccessManagerImpl implements LocalAccessManager
{
    
    private static final String SSH_ENABLE_SCRIPT  = BUNNICULA_BASE + "/ssh_enable.sh";
    private static final String SSH_DISABLE_SCRIPT = BUNNICULA_BASE + "/ssh_disable.sh";

    private final Logger logger = Logger.getLogger(getClass());

    AccessSettingsInternal accessSettings = null;

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
            new DeletingDataSaver<AccessSettings>( MvvmContextFactory.context(), "AccessSettings" );

        AccessSettingsInternal newSettings = AccessSettingsInternal.makeInstance( settings );
        saver.saveData( newSettings.toSettings());
        this.accessSettings = newSettings;

        setSshAccess( this.accessSettings );
    }

    /* ---------------------- PACKAGE ---------------------- */
    /* Initialize the settings, load at startup */
    synchronized void init()
    {
        DataLoader<AccessSettings> loader =
            new DataLoader<AccessSettings>( "AccessSettings", MvvmContextFactory.context());

        AccessSettings settings = loader.loadData();
        
        if ( settings == null ) {
            logger.info( "There are no access settings in the database, must initialize from files." );
            
            setSettings( NetworkConfigurationLoader.getInstance().loadAccessSettings(), true );
        } else {
            this.accessSettings = AccessSettingsInternal.makeInstance( settings );
        }
    }
    
    /* Invoked at the very end to actually update the settings */
    /* This should also be synchronized from its callers. */
    synchronized void commit( ScriptWriter scriptWriter )
    {
        /* Save the variables that need to be written to networking.sh */
        updateShellScript( scriptWriter, this.accessSettings );
    }

    /* ---------------------- PRIVATE ---------------------- */
            private void updateShellScript( ScriptWriter scriptWriter, AccessSettingsInternal access )
    {
        if ( access == null ) {
            logger.warn( "unable to save hostname, access settings are not initialized." );            
            return;
        }

        scriptWriter.appendVariable( FLAG_HTTP_IN, access.getIsInsideInsecureEnabled());
        scriptWriter.appendVariable( FLAG_HTTPS_OUT, access.getIsOutsideAccessEnabled());
        scriptWriter.appendVariable( FLAG_HTTPS_RES, access.getIsOutsideAccessRestricted());

        IPaddr outsideNetwork = access.getOutsideNetwork();
        IPaddr outsideNetmask = access.getOutsideNetmask();
        
        if (( outsideNetwork != null ) && !outsideNetwork.isEmpty()) {
            scriptWriter.appendVariable( FLAG_OUT_NET, outsideNetwork.toString());
            
            if (( outsideNetmask != null ) && !outsideNetmask.isEmpty()) {
                scriptWriter.appendVariable( FLAG_OUT_MASK, outsideNetmask.toString());
            }
        }
    }

    private void setSshAccess( AccessSettingsInternal access )
    {
        if ( !NetworkManagerImpl.getInstance().getSaveSettings()) {
            logger.warn( "not modifying SSH access as requested." );
            return;
        }

        if ( access == null ) {
            logger.warn( "unable to save hostname, address settings are not initialized." );            
            return;
        }
        
        try {
            if ( access.getIsSshEnabled()) {
                ScriptRunner.getInstance().exec( SSH_ENABLE_SCRIPT );
            } else {
                ScriptRunner.getInstance().exec( SSH_DISABLE_SCRIPT );
            }
        } catch ( Exception ex ) {
            logger.error( "Unable to configure ssh", ex );
        }
    }
}

