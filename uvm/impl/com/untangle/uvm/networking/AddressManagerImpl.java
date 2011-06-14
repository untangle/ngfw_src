/* $HeadURL$ */
package com.untangle.uvm.networking;

import static com.untangle.uvm.networking.ShellFlags.FLAG_EXTERNAL_HTTPS_PORT;
import static com.untangle.uvm.networking.ShellFlags.FLAG_EXTERNAL_PUBLIC_ADDR;
import static com.untangle.uvm.networking.ShellFlags.FLAG_EXTERNAL_PUBLIC_ENABLED;
import static com.untangle.uvm.networking.ShellFlags.FLAG_EXTERNAL_PUBLIC_HTTPS_PORT;
import static com.untangle.uvm.networking.ShellFlags.FLAG_PUBLIC_URL;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.hibernate.Session;
import org.hibernate.Query;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.UvmState;
import com.untangle.uvm.node.HostAddress;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.HostAddress;
import com.untangle.uvm.node.script.ScriptWriter;
import com.untangle.uvm.util.JsonClient;
import com.untangle.uvm.util.XMLRPCUtil;
import com.untangle.uvm.util.TransactionWork;

class AddressManagerImpl implements LocalAddressManager
{
    private static final String PROPERTY_COMMENT    = "Properties for the https port at startup.";

    private final Logger logger = Logger.getLogger(getClass());

    private AddressSettings addressSettings = null;

    public AddressManagerImpl()
    {
        init();
    }
    
    /* Use this to retrieve address settings */
    public AddressSettings getSettings()
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
    @SuppressWarnings("unchecked")
    public void setSettings( final AddressSettings settings, boolean forceSave, boolean updateSuffix )
    {
        logger.debug( "Got the settings: " + settings.toString());

        TransactionWork<Void> tw = new TransactionWork<Void>()
            {
                public boolean doWork(Session s)
                {
                    /* delete old settings */
                    Query q = s.createQuery( "from " + "AddressSettings" );
                    for ( Iterator<AddressSettings> iter = q.iterate() ; iter.hasNext() ; ) {
                        AddressSettings oldSettings = iter.next();
                        if (((long)settings.getId()) != ((long)oldSettings.getId())) 
                            s.delete( oldSettings );
                    }

                    addressSettings = (AddressSettings)s.merge(settings);
                    return true;
                }
            };
        LocalUvmContextFactory.context().runTransaction(tw);
        
        synchronized ( this ) {
            /* Rebind https to the new port */
            rebindHttps( this.addressSettings );
        }
    }

    /* ---------------------- PACKAGE ---------------------- */
    /* Initialize the settings, load at startup */
    synchronized void init()
    {
        TransactionWork<Object> tw = new TransactionWork<Object>()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery( "from " + "AddressSettings");
                    addressSettings = (AddressSettings)q.uniqueResult();
                    
                    return true;
                }
            };

        LocalUvmContextFactory.context().runTransaction(tw);
        
        if ( this.addressSettings == null ) {
            logger.warn( "There are no address settings in the database, initializing" );
            AddressSettings settings = new AddressSettings();
            /* load reasonable defaults */
            settings.setHttpsPort( NetworkUtil.DEF_HTTPS_PORT );
            settings.setIsHostNamePublic( false );
            settings.setIsPublicAddressEnabled( false );
            settings.setHostName( NetworkUtil.DEFAULT_HOSTNAME );
            /* try to retrieve the settings from the configuration files */

            setSettings( settings, true );
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

    private void updateShellScript( ScriptWriter scriptWriter, AddressSettings address )
    {
        if ( address == null ) {
            logger.warn( "unable to complete shell script, address settings are not initialized." );
            return;
        }

        /* The address and port of the external router that is in front of us */
        IPAddress ip = NetworkUtil.BOGUS_DHCP_ADDRESS;

        HostAddress currentAddress = this.addressSettings.getCurrentPublicAddress();
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
        
        scriptWriter.appendLine("# Public IP of Untangle server (if set)");
        scriptWriter.appendVariable( FLAG_EXTERNAL_PUBLIC_ADDR, ip.toString());

        scriptWriter.appendLine("# Public Port of Untangle server (if set)");
        scriptWriter.appendVariable( FLAG_EXTERNAL_PUBLIC_HTTPS_PORT, String.valueOf( port ));

        scriptWriter.appendLine("# Public URL of Untangle server");
        scriptWriter.appendVariable( FLAG_PUBLIC_URL, this.addressSettings.getCurrentURL());
        
        /* This is used to know what to redirect to 443 */
        scriptWriter.appendLine("# HTTPS port of Untangle server");
        scriptWriter.appendVariable( FLAG_EXTERNAL_HTTPS_PORT, this.addressSettings.getHttpsPort());


        if ( this.addressSettings.getIsPublicAddressEnabled()) {
            scriptWriter.appendLine("# Are Public IP/Port settingsenabled?");
            scriptWriter.appendVariable( FLAG_EXTERNAL_PUBLIC_ENABLED, "true" );
        }        
    }

    private void rebindHttps( AddressSettings address )
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
