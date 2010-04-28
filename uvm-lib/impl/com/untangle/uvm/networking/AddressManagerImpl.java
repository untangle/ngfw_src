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

import static com.untangle.uvm.networking.ShellFlags.FILE_PROPERTIES;
import static com.untangle.uvm.networking.ShellFlags.FLAG_EXTERNAL_HTTPS_PORT;
import static com.untangle.uvm.networking.ShellFlags.FLAG_EXTERNAL_ROUTER_ADDR;
import static com.untangle.uvm.networking.ShellFlags.FLAG_EXTERNAL_ROUTER_EN;
import static com.untangle.uvm.networking.ShellFlags.FLAG_EXTERNAL_ROUTER_PORT;
import static com.untangle.uvm.networking.ShellFlags.FLAG_PUBLIC_URL;
import static com.untangle.uvm.networking.ShellFlags.PROPERTY_HTTPS_PORT;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.UvmState;
import com.untangle.uvm.networking.internal.AddressSettingsInternal;
import com.untangle.uvm.node.HostAddress;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.script.ScriptWriter;
import com.untangle.uvm.util.DataLoader;
import com.untangle.uvm.util.DataSaver;
import com.untangle.uvm.util.DeletingDataSaver;
import com.untangle.uvm.util.JsonClient;
import com.untangle.uvm.util.XMLRPCUtil;

class AddressManagerImpl implements LocalAddressManager
{
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
    public void setSettings( AddressSettings settings )
    {
        setSettings( settings, false, false );
    }

    public void setSettings( AddressSettings settings, boolean forceSave )
    {
        setSettings( settings, forceSave, false );
    }

    void setWizardSettings( AddressSettings settings )
    {
        setSettings( settings, true, true );
    }

    /* Use this to modify the address settings without modifying the network settings */
    /* @param updateSuffix Set to true to also update the search domain. */
    public void setSettings( AddressSettings settings, boolean forceSave, boolean updateSuffix )
    {
        logger.debug( "Got the settings: " + settings.toString());

        /* should validate settings */
        if ( !forceSave && settings.isClean()) logger.debug( "settings are clean, leaving alone." );
        
        synchronized ( this ) {
            /* Need to save the settings to the database, then update the
             * local value, everything is executed later */
            DataSaver<AddressSettings> saver = 
                new DeletingDataSaver<AddressSettings>( LocalUvmContextFactory.context(), "AddressSettings" );

            AddressSettingsInternal newSettings = calculatePublicAddress( settings );
            
            saver.saveData( newSettings.toSettings());
            this.addressSettings = newSettings;
                        
            /* Save the properties */
            saveProperties( this.addressSettings );
            
            /* Rebind https to the new port */
            rebindHttps( this.addressSettings );
            
            /* Call the listeners */
            callListeners( this.addressSettings );
        }
        
        /* Save the hostname */
        setHostName( this.addressSettings, updateSuffix );
    }

    /* ---------------------- PACKAGE ---------------------- */
    /* Initialize the settings, load at startup */
    synchronized void init()
    {
        DataLoader<AddressSettings> loader =
            new DataLoader<AddressSettings>( "AddressSettings", LocalUvmContextFactory.context());

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
        
        saveProperties( this.addressSettings );
    }

    /* Used to refresh the value of the public address */
    synchronized void updateAddress( Properties properties )
    {
        AddressSettings settings = this.addressSettings.toSettings();
        String hostname = properties.getProperty( "com.untangle.networking.hostname" );
        try {
            if ( hostname == null ) {
                settings.setHostName( HostName.parse( LocalUvmContextFactory.context().brandingManager().getCompanyName().toLowerCase() + ".example.com") );
            }
            else {
                settings.setHostName( HostName.parse( hostname ));
            }
        } catch ( ParseException e ) {
            logger.warn( "Unable to parse the hostname: '" + hostname + "'", e );
            settings.setHostName( null );
        }
        
        this.addressSettings = calculatePublicAddress( settings );

        if ( logger.isDebugEnabled()) {
            logger.debug( "New Address Settings:\n" + this.addressSettings );
        }

        /* Call the listeners */
        callListeners( this.addressSettings );
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
            
            /* If a hostname is available, and qualified, then use the hostname */
            if (( name != null ) && !name.isEmpty() && name.isQualified()) {
                address = new HostAddress( primaryAddress, name );
            }
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
        scriptWriter.appendVariable( FLAG_PUBLIC_URL, this.addressSettings.getCurrentURL());
        
        /* This is used to know what to redirect to 443 */
        scriptWriter.appendVariable( FLAG_EXTERNAL_HTTPS_PORT, this.addressSettings.getHttpsPort());
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

    private void setHostName( AddressSettingsInternal address, boolean updateSuffix )
    {
        if ( !NetworkManagerImpl.getInstance().getSaveSettings()) {
            logger.warn( "not saving hostname settings as requested." );
            return;
        }

        if ( address == null ) {
            logger.warn( "unable to save hostname, address settings are not initialized." );            
            return;
        }

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put( "hostname", address.getHostName().toString());
            jsonObject.put( "save_suffix", updateSuffix );
        } catch ( JSONException e ) {
            logger.warn( "Unable to save JSON object", e );
            return;
        }
        
        /* Make a synchronous request */
        try {
            /* This will save the hostname but not commit it to the O/S, have to commit it
             * when there is no locks. */
            JsonClient.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, "save_hostname", jsonObject );
        } catch ( Exception e ) {
            logger.warn( "Unable to save the hostname", e );
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
            logger.warn("Setting port");
            LocalUvmContextFactory.context().localAppServerManager().rebindExternalHttpsPort( port );
            logger.warn("Done Setting port");
        } catch ( Exception e ) {
            if ( !LocalUvmContextFactory.context().state().equals( UvmState.RUNNING )) {
                logger.info( "unable to rebind port at startup, expected. " + e );
            } else {
                logger.warn( "unable to rebind https to port: " + port, e );
            }
        }
    }
}
