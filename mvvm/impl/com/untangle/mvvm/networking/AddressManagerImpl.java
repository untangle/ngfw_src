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

import java.io.FileOutputStream;
import java.io.File;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmState;


import com.untangle.mvvm.tran.HostAddress;
import com.untangle.mvvm.tran.HostName;
import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.script.ScriptWriter;
import com.untangle.mvvm.tran.script.ScriptRunner;

import com.untangle.mvvm.util.DataLoader;
import com.untangle.mvvm.util.DataSaver;
import com.untangle.mvvm.util.DeletingDataSaver;

import com.untangle.mvvm.networking.internal.AddressSettingsInternal;

import static com.untangle.mvvm.networking.NetworkManagerImpl.BUNNICULA_BASE;

import static com.untangle.mvvm.networking.ShellFlags.FLAG_EXTERNAL_ROUTER_ADDR;
import static com.untangle.mvvm.networking.ShellFlags.FLAG_EXTERNAL_ROUTER_EN;
import static com.untangle.mvvm.networking.ShellFlags.FLAG_EXTERNAL_ROUTER_PORT;

import static com.untangle.mvvm.networking.ShellFlags.FILE_PROPERTIES;
import static com.untangle.mvvm.networking.ShellFlags.PROPERTY_HTTPS_PORT;

class AddressManagerImpl implements LocalAddressManager
{
    private static final String HOSTNAME_SCRIPT    = BUNNICULA_BASE + "/networking/save-hostname";

    private static final String PROPERTY_COMMENT    = "Properties for the https port at startup.";

    private final Logger logger = Logger.getLogger(getClass());

    private AddressSettingsInternal addressSettings = null;

    private Set<AddressSettingsListener> listeners = new HashSet<AddressSettingsListener>();

    AddressManagerImpl()
    {
    }
    
    /* Use this to retrieve address settings */
    public AddressSettings getSettings()
    {
        return this.addressSettings.toSettings();
    }

    public AddressSettingsInternal getInternalSettings()
    {
        return this.addressSettings;
    }

    /* Use this to modify the address settings without modifying the network settings */
    public synchronized void setSettings( AddressSettings settings )
    {
        setSettings( settings, false );
    }

    /* Use this to modify the address settings without modifying the network settings */
    public synchronized void setSettings( AddressSettings settings, boolean forceSave )
    {
        /* should validate settings */
        if ( !forceSave && settings.isClean()) logger.debug( "settings are clean, leaving alone." );
        
        /* Need to save the settings to the database, then update the
         * local value, everything is executed later */
        DataSaver<AddressSettings> saver = 
            new DeletingDataSaver<AddressSettings>( MvvmContextFactory.context(), "AddressSettings" );

        AddressSettingsInternal newSettings = calculatePublicAddress( settings );
        
        saver.saveData( newSettings.toSettings());
        this.addressSettings = newSettings;

        /* Save the hostname */
        setHostName( this.addressSettings );

        /* Save the properties */
        saveProperties( this.addressSettings );

        /* Rebind https to the new port */
        rebindHttps( this.addressSettings );

        /* Call the listeners */
        callListeners( this.addressSettings );
    }

    /* ---------------------- PACKAGE ---------------------- */
    /* Initialize the settings, load at startup */
    synchronized void init()
    {
        DataLoader<AddressSettings> loader =
            new DataLoader<AddressSettings>( "AddressSettings", MvvmContextFactory.context());

        AddressSettings settings = loader.loadData();
        
        if ( settings == null ) {
            logger.info( "There are no address settings in the database, must initialize from files." );
            
            setSettings( NetworkConfigurationLoader.getInstance().loadAddressSettings(), true );
        } else {
            this.addressSettings = calculatePublicAddress( settings );
        }
    }

    /* Invoked at the very end to actually make the necessary changes for the settings to take effect. */
    /* This should also be synchronized from its callers. */
    synchronized void commit( ScriptWriter scriptWriter )
    {
        if ( this.addressSettings == null ) {
            logger.warn( "AddressSettings have not been initialized, not saving settings." );
            return;
        }
        
        updateShellScript( scriptWriter, this.addressSettings );
    }

    /* Used to refresh the value of the public address */
    synchronized void updateAddress()
    {
        this.addressSettings = calculatePublicAddress( this.addressSettings.toSettings());

        /* xxx not sure if it should call the listeners here, most of
         * them are concerned with the hostname, so i don't think
         * so. */
    }

    /* Register a listener to be called whenever there are changes to the hostname/address */
    synchronized void registerListener( AddressSettingsListener listener )
    {
        this.listeners.add( listener );
    }

    synchronized void unregisterListener( AddressSettingsListener listener )
    {
        this.listeners.remove( listener );
    }

    /* ---------------------- PRIVATE ---------------------- */
    private AddressSettingsInternal calculatePublicAddress( AddressSettings settings )
    {
        NetworkManagerImpl nm = NetworkManagerImpl.getInstance();

        IPaddr primaryAddress = nm.getPrimaryAddress();

        HostAddress address = null;
        int port = settings.getHttpsPort();

        if ( primaryAddress == null ) {
            logger.warn( "null primary address, may be unable to initialize the public address." );
            primaryAddress = NetworkUtil.BOGUS_DHCP_ADDRESS;
        }

        /* if using the public address then, get the address from the settings. */
        String publicAddress = settings.getPublicAddress();

        /* has public address trumps over all other settings */
        if ( settings.getIsPublicAddressEnabled() && ( publicAddress != null ) && 
             ( publicAddress.trim().length() > 0 )) {
            /* has public address is set and a public address is available */
            address = new HostAddress( settings.getPublicIPaddr());
            port = settings.getPublicPort();
        } else if ( settings.getIsHostNamePublic() || nm.isDynamicDnsEnabled())  {
            /* no public address, use the primary address, and the hostname */
            HostName name = settings.getHostName();
            
            /* If a hostname is available, then use the hostname */
            if (( name != null ) && !name.isEmpty()) address = new HostAddress( primaryAddress, name );
        }

        /* If none of the other condititions have been met, just use the primary address */
        if ( address == null ) {
            address = new HostAddress( primaryAddress );
        }

        if ( port <= 0 || port >= 0xFFFF ) {
            logger.warn( "port is an invalid value, using default: " + port );
            port = NetworkUtil.DEF_HTTPS_PORT;
        }

        /* last check, there must be an ip assigned */
        if ( address.getIp() == null ) {
            /* This shouldn't happen */
            logger.warn( "null ip address, recreating HostAddress." );
            address = new HostAddress( primaryAddress, address.getHostName());
        }
        
        return AddressSettingsInternal.makeInstance( settings, address, port );
    }

    private synchronized void callListeners( AddressSettingsInternal address )
    {
        if ( address == null ) {
            logger.info( "null settings, not calling listeners" );
            return;
        }

        for ( AddressSettingsListener listener : this.listeners ) {
            try {
                listener.event( address );
            } catch ( Exception e ) {
                logger.error( "Exception calling listener", e );
            }
        }
    }

    private void updateShellScript( ScriptWriter scriptWriter, AddressSettingsInternal address )
    {
        if ( address == null ) {
            logger.warn( "unable to complete shell script, address settings are not initialized." );
            return;
        }

        /* The address and port of the external router that is in front of us */
        IPaddr ip = NetworkUtil.BOGUS_DHCP_ADDRESS;

        HostAddress currentAddress = this.addressSettings.getCurrentAddress();
        if (( currentAddress != null ) && ( currentAddress.getIp() != null ) && 
            !currentAddress.getIp().isEmpty()) {
            ip = currentAddress.getIp();
        } else {
            logger.warn( "The current address is not properly initialized: <" + currentAddress + ">" );
        }

        int port = this.addressSettings.getCurrentPublicPort();
        if ( port < 0 || port > 0xFFFF ) {
            logger.warn( "The current port is not properly initialized: <" + port + ">" );
            port = 443;
        }
        
        scriptWriter.appendVariable( FLAG_EXTERNAL_ROUTER_ADDR, ip.toString());
        scriptWriter.appendVariable( FLAG_EXTERNAL_ROUTER_PORT, String.valueOf( port ));
        
        if ( this.addressSettings.getIsPublicAddressEnabled()) {
            scriptWriter.appendVariable( FLAG_EXTERNAL_ROUTER_EN, "true" );
        }        
    }

    private void saveProperties( AddressSettingsInternal address )
    {
        if ( address == null ) {
            logger.warn( "unable to save properties, address settings are not initialized." );
        }

        int port = address.getHttpsPort();
        
        Properties properties = new Properties();
        
        if (( port > 0 ) && ( port < 0xFFFF )) properties.setProperty( PROPERTY_HTTPS_PORT, "" + port );
        
        try {
            logger.debug( "Storing properties into: " + FILE_PROPERTIES + "[" + port + "]" );
            properties.store( new FileOutputStream( new File( FILE_PROPERTIES )), PROPERTY_COMMENT );
        } catch ( Exception e ) {
            logger.error( "Error saving HTTPS port" );
        }

    }

    private void setHostName( AddressSettingsInternal address )
    {
        if ( !NetworkManagerImpl.getInstance().getSaveSettings()) {
            logger.warn( "not saving hostname settings as requested." );
            return;
        }

        if ( address == null ) {
            logger.warn( "unable to save hostname, address settings are not initialized." );            
            return;
        }
        
        try {
            ScriptRunner.getInstance().exec( HOSTNAME_SCRIPT, address.getHostName().toString());
        } catch ( Exception e ) {
            logger.error( "unable to save the hostname." );
        }
    }

    private void rebindHttps( AddressSettingsInternal address )
    {
        if ( address == null ) {
            logger.warn( "unable to rebind https port, address settings are not initialized." );
            return;
        }

        int port = address.getHttpsPort();

        try {
            MvvmContextFactory.context().appServerManager().rebindExternalHttpsPort( port );
        } catch ( Exception e ) {
            if ( !MvvmContextFactory.context().state().equals( MvvmState.RUNNING )) {
                logger.info( "unable to rebind port at startup, expected. " + e );
            } else {
                logger.warn( "unable to rebind https to port: " + port, e );
            }
        }
    }
}
