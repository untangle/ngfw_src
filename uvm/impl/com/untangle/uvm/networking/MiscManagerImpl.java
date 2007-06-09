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

package com.untangle.uvm.networking;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.networking.internal.MiscSettingsInternal;
import com.untangle.uvm.node.script.ScriptWriter;
import com.untangle.uvm.util.DataLoader;
import com.untangle.uvm.util.DataSaver;
import com.untangle.uvm.util.DeletingDataSaver;
import org.apache.log4j.Logger;

import static com.untangle.uvm.networking.ShellFlags.FLAG_TCP_WIN;
import static com.untangle.uvm.networking.ShellFlags.FLAG_POST_FUNC;
import static com.untangle.uvm.networking.ShellFlags.POST_FUNC_NAME;
import static com.untangle.uvm.networking.ShellFlags.DECL_POST_CONF;

import static com.untangle.uvm.networking.ShellFlags.FLAG_CUSTOM_RULES;
import static com.untangle.uvm.networking.ShellFlags.CUSTOM_RULES_NAME;
import static com.untangle.uvm.networking.ShellFlags.DECL_CUSTOM_RULES;

class MiscManagerImpl implements LocalMiscManager
{
    private final Logger logger = Logger.getLogger(getClass());

    MiscSettingsInternal miscSettings = null;

    MiscManagerImpl()
    {
    }

    /* Use this to retrieve just the remote settings */
    public MiscSettings getSettings()
    {
        return this.miscSettings.toSettings();
    }

    public MiscSettingsInternal getInternalSettings()
    {
        return this.miscSettings;
    }

    /* Use this to mess with the remote settings without modifying the network settings */
    public synchronized void setSettings( MiscSettings settings )
    {
        setSettings( settings, false );
    }

    /* Use this to mess with the remote settings without modifying the network settings */
    public synchronized void setSettings( MiscSettings settings, boolean forceSave )
    {
        /* should validate settings */
        if ( !forceSave && settings.isClean()) logger.debug( "settings are clean, leaving alone." );

        /* Need to save the settings to the database, then update the
         * local value, everything is executed later */
        DataSaver<MiscSettings> saver =
            new DeletingDataSaver<MiscSettings>( UvmContextFactory.context(), "MiscSettings" );

        MiscSettingsInternal newSettings = MiscSettingsInternal.makeInstance( settings );
        saver.saveData( newSettings.toSettings());
        this.miscSettings = newSettings;
    }

    /* ---------------------- PACKAGE ---------------------- */
    /* Initialize the settings, load at startup */
    synchronized void init()
    {
        DataLoader<MiscSettings> loader =
            new DataLoader<MiscSettings>( "MiscSettings", UvmContextFactory.context());

        MiscSettings settings = loader.loadData();

        if ( settings == null ) {
            logger.info( "There are no misc settings in the database, must initialize from files." );

            setSettings( NetworkConfigurationLoader.getInstance().loadMiscSettings(), true );
        } else {
            this.miscSettings = MiscSettingsInternal.makeInstance( settings );
        }
    }

    /* Invoked at the very end to actually make the necessary changes for the settings to take effect. */
    /* This should also be synchronized from its callers. */
    synchronized void commit( ScriptWriter scriptWriter )
    {
        updateShellScript( scriptWriter, this.miscSettings );
    }

    /* ---------------------- PRIVATE ---------------------- */
    private void updateShellScript( ScriptWriter scriptWriter, MiscSettingsInternal misc )
    {
        if ( misc == null ) {
            logger.warn( "Miscellaneous settings are not initialized, unable to update shell script." );
            return;
        }

        /* Append the variables that need to be written to networking.sh */
        scriptWriter.appendVariable( FLAG_TCP_WIN, misc.getIsTcpWindowScalingEnabled());

        /* append the post configuration script if it exist */
        addFunction( scriptWriter, misc.getPostConfigurationScript(),
                     FLAG_POST_FUNC, POST_FUNC_NAME, DECL_POST_CONF );

        /* append the custom rules if they exist. */
        addFunction( scriptWriter, misc.getCustomRules(),
                     FLAG_CUSTOM_RULES, CUSTOM_RULES_NAME, DECL_CUSTOM_RULES );
    }
    private void addFunction( ScriptWriter scriptWriter,
                              String script, String flag, String name, String declaration )
    {
        /* no script, return */
        if ( script == null ) return;

        script = script.trim();
        /* empty script, return */
        if ( script.length() == 0 ) return;

        /* append the script to the string builder */
        scriptWriter.appendLine( declaration );
        scriptWriter.appendLine( script );
        scriptWriter.appendLine( "}\n" );

        /* Append the flag to indicate that there is a postConfiguration script */
        scriptWriter.appendVariable( flag, name );
    }
}
