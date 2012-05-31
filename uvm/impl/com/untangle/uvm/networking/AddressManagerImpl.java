/** 
 * $Id$
 */
package com.untangle.uvm.networking;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmState;
import com.untangle.uvm.node.HostAddress;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.HostAddress;
import com.untangle.uvm.util.JsonClient;
import com.untangle.uvm.util.XMLRPCUtil;

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
        logger.info( "setSettings( " + settings.getPublicAddress() + ":" + settings.getHttpsPort() + ")" );

        this.addressSettings = settings;
        
//         TransactionWork<Void> tw = new TransactionWork<Void>()
//             {
//                 public boolean doWork(NodeSession s)
//                 {
//                     /* delete old settings */
//                     Query q = s.createQuery( "from " + "AddressSettings" );
//                     for ( Iterator<AddressSettings> iter = q.iterate() ; iter.hasNext() ; ) {
//                         AddressSettings oldSettings = iter.next();
//                         s.delete( oldSettings );
//                     }

//                     try{
//                         addressSettings = (AddressSettings)s.merge(settings);
//                     } catch (org.hibernate.ObjectNotFoundException exc) {
//                         s.save(settings);
//                         addressSettings = settings;
//                     }
//                     return true;
//                 }
//             };
//         UvmContextFactory.context().runTransaction(tw);
        
        synchronized ( this ) {
            /* Rebind https to the new port */
            rebindHttps( this.addressSettings );
        }
    }

    /* ---------------------- PACKAGE ---------------------- */
    /* Initialize the settings, load at startup */
    synchronized void init()
    {
//         TransactionWork<Object> tw = new TransactionWork<Object>()
//             {
//                 public boolean doWork(NodeSession s)
//                 {
//                     Query q = s.createQuery( "from " + "AddressSettings");
//                     addressSettings = (AddressSettings)q.uniqueResult();
                    
//                     return true;
//                 }
//             };

//         UvmContextFactory.context().runTransaction(tw);
        
        if ( this.addressSettings == null ) {
            logger.warn( "There are no address settings in the database, initializing" );
            AddressSettings settings = new AddressSettings();
            /* load reasonable defaults */
            settings.setHttpsPort( NetworkUtil.DEF_HTTPS_PORT );
            settings.setIsHostNamePublic( false );
            settings.setIsPublicAddressEnabled( false );
            /* try to retrieve the settings from the configuration files */

            setSettings( settings, true );
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
            logger.info("Rebinding HTTPS port: " + port);
            UvmContextFactory.context().localAppServerManager().rebindExternalHttpsPort( port );
            logger.info("Rebinding HTTPS port done.");
        } catch ( Exception e ) {
            if ( !UvmContextFactory.context().state().equals( UvmState.RUNNING )) {
                logger.info( "unable to rebind port at startup, expected. ");
            } else {
                logger.warn( "unable to rebind https to port: " + port, e );
            }
        }
    }
}
