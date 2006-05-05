/*
 * Copyright (c) 2003, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.argon;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Properties;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.log4j.Logger;

import com.metavize.jnetcap.Netcap;
import com.metavize.jnetcap.InterfaceData;
import com.metavize.jnetcap.Shield;
import com.metavize.jnetcap.JNetcapException;

import com.metavize.mvvm.engine.PolicyManagerPriv;

import com.metavize.mvvm.ArgonManager;
import com.metavize.mvvm.NetworkingConfiguration;

import com.metavize.mvvm.tran.firewall.InterfaceRedirect;

import com.metavize.mvvm.shield.ShieldNodeSettings;

public class ArgonManagerImpl implements ArgonManager
{    
    private static final Shield shield = Shield.getInstance();

    private static final String BUNNICULA_CONF      = System.getProperty( "bunnicula.conf.dir" );
    static final String TRANSFORM_INTF_FILE     = BUNNICULA_CONF + "/argon.properties";
    static final String PROPERTY_TRANSFORM_INTF = "argon.userintf";
    private static final String PROPERTY_COMMENT = "Transform interfaces (eg VPN)";

    private static final String BOGUS_OUTSIDE_ADDRESS_STRING = "169.254.210.51";
    private static final String BOGUS_INSIDE_ADDRESS_STRING  = "169.254.210.52";

    private static final InetAddress BOGUS_OUTSIDE_ADDRESS;
    private static final InetAddress BOGUS_INSIDE_ADDRESS;

    private static final ArgonManagerImpl INSTANCE = new ArgonManagerImpl();
    
    private static final List<InterfaceData> EMPTY_INTF_DATA_LIST = Collections.emptyList();
   

    private final Logger logger = Logger.getLogger( ArgonManagerImpl.class );

    private final Netcap netcap = Netcap.getInstance();
    
    private List<InterfaceData> insideIntfDataList  = EMPTY_INTF_DATA_LIST;
    private List<InterfaceData> outsideIntfDataList = EMPTY_INTF_DATA_LIST;

    private String internalBridgeIntf = "";

    private PolicyManagerPriv policyManager;

    private boolean isShutdown = false;
        
    private ArgonManagerImpl()
    {
    }

    public void shieldStatus( InetAddress ip, int port, int interval )
    {
        if ( port < 0 || port > 0xFFFF ) {
            throw new IllegalArgumentException( "Invalid port: " + port );
        }

        do {
            shield.status( ip, port );

            try {
                if ( interval > 0 ) Thread.sleep( interval );
            } catch ( InterruptedException e ) {
                logger.debug( "Shield status interrupted" );
                break;
            }
        } while ( interval > 0 );
    }

    public void shieldReconfigure()
    {
        String shieldFile = Argon.getInstance().shieldFile;
        if ( shieldFile  != null ) {
            shield.config( shieldFile );
        }
    }

    /* Indicate that the shutdown process has started, this is used to prevent NAT from both
     * re-enabling the bridge during shutdown, Argon will do that automatically.
     * This also prevents transforms from registering or deregistering interfaces after shutdown 
     */
    synchronized void isShutdown() 
    {
        isShutdown = true;
    }    
    
    public String getInside() throws ArgonException
    {
        return IntfConverter.getInstance().argonIntfToString( IntfConverter.INSIDE );
    }

    public InetAddress getInsideAddress()
    {
        if ( this.insideIntfDataList.size() < 1 ) return null;
        return insideIntfDataList.get( 0 ).getAddress();
    }

    public InetAddress getInsideNetmask()
    {
        if ( this.insideIntfDataList.size() < 1 ) return null;
        return insideIntfDataList.get( 0 ).getNetmask();
    }

    public String getOutside() throws ArgonException
    {
        return IntfConverter.getInstance().argonIntfToString( IntfConverter.OUTSIDE );
    }
        
    /* Set the interface override list. */
    public void setInterfaceOverrideList( List<InterfaceRedirect> overrideList )
    {
        InterfaceOverride.getInstance().setOverrideList( overrideList );
    }

    /* Set the interface override list. */
    public void clearInterfaceOverrideList()
    {
        InterfaceOverride.getInstance().clearOverrideList();
    }
    
    /* Get the outgoing argon interface for an IP address */
    public byte getOutgoingInterface( InetAddress destination ) throws ArgonException
    {
        try {
            byte netcapIntf = netcap.getOutgoingInterface( destination );
            return IntfConverter.toArgon( netcapIntf );
        } catch ( JNetcapException e ) {
            throw new ArgonException( e );
        }
    }

    /* Update the shield node settings */
    public void setShieldNodeSettings( List<ShieldNodeSettings> shieldNodeSettingsList ) 
        throws ArgonException
    {
        List <com.metavize.jnetcap.ShieldNodeSettings> settingsList = 
            new LinkedList<com.metavize.jnetcap.ShieldNodeSettings>();

        for ( ShieldNodeSettings settings : shieldNodeSettingsList ) {
            InetAddress netmask;
            try {
                netmask = InetAddress.getByAddress( new byte[]{ (byte)255, (byte)255, (byte)255, (byte)255 } );
            } catch( UnknownHostException e ) {
                logger.error( "Unable to parse default netmask", e );
                throw new ArgonException( e );
            }

            if ( settings == null ) {
                logger.error( "NULL Settings in list\n" );
                continue;
            }

            if ( !settings.isLive()) {
                logger.debug( "Ignoring disabled settings" );
                continue;
            }

            if ( settings.getAddress() == null || settings.getAddress().isEmpty()) {
                logger.error( "Settings with empty address, ignoring" );
                continue;
            }
            
            if ( settings.getNetmask() != null && !settings.getNetmask().isEmpty()) {
                logger.warn( "Settings with non-empty netmask, ignoring netmask using 255.255.255.255" );
            }
            
            

            
            logger.debug( "Adding shield node setting " + settings.getAddress().toString() + "/" + 
                          netmask.getHostAddress() + " divider: " + settings.getDivider());
                        
            settingsList.add( new com.metavize.jnetcap.ShieldNodeSettings( settings.getDivider(), 
                                                                           settings.getAddress().getAddr(), 
                                                                           netmask ));
        }

        try {
            Shield.getInstance().setNodeSettings( settingsList );
        } catch ( Exception e ) {
            throw new ArgonException( "Unable to set the shield node settingss", e );
        }
    }

    private void updateIntfArray() throws ArgonException
    {
        IntfConverter ic = IntfConverter.getInstance();

        /* Update the netcap interface array */
        try {
            Netcap.getInstance().configureInterfaceArray( ic.netcapIntfArray(), ic.deviceNameArray());
        }  catch ( JNetcapException e ) {
            throw new ArgonException( e );
        }

        /* List, _ seperated, of interfaces and their corresponding device */
        /* Write out the list of user interfaces */
        
        List<TransformInterface> til = ic.transformInterfaceList();

        String list = "";
        for ( TransformInterface ti : til ) {
            if ( list.length() > 0 ) list += "_";
            list += ti.deviceName() + "," + ti.argonIntf();
        }

        Properties properties = new Properties();
        properties.setProperty( PROPERTY_TRANSFORM_INTF, list );

        try {
            logger.debug( "Storing properties into: " + TRANSFORM_INTF_FILE );
            properties.store( new FileOutputStream( new File( TRANSFORM_INTF_FILE )), PROPERTY_COMMENT );
        } catch ( Exception e ) {
            logger.error( "Unable to write transform interface properties:" + TRANSFORM_INTF_FILE, e );
        }
        
        /* Update the policy manager */
        if ( ic.clearUpdatePolicyManager()) {
            this.policyManager.reconfigure( ic.argonIntfArray());
        }
    }

    /* Interface management function */
    void initializeIntfArray( PolicyManagerPriv policyManager, String inside, String outside, 
                              String dmz, String userIntfs ) 
        throws ArgonException
    {
        this.policyManager = policyManager;

        /* Initialize the interface array */
        IntfConverter.getInstance().init( outside, inside, dmz, userIntfs );

        updateIntfArray();
    }

    public void registerIntf( byte argonIntf, String deviceName )
        throws ArgonException
    {
        if ( isShutdown ) {
            logger.info( "Already shutdown, unable to register an interface" );
            return;
        }
        
        if ( !IntfConverter.getInstance().registerIntf( argonIntf, deviceName )) {
            logger.debug( "Ignoring interface that is already registered" );
            return;
        }
        
        updateIntfArray();        
    }
    
    public void deregisterIntf( byte argonIntf )
        throws ArgonException
    {
        if ( isShutdown ) {
            logger.info( "Already shutdown, unable to deregister interface" );
            return;
        }

        if ( !IntfConverter.getInstance().deregisterIntf( argonIntf )) {
            logger.debug( "Ignoring interface that is not registered" );
            return;
        }

        updateIntfArray();
    }

    /** Add an interface to the internal bridge */
    public void enableInternalBridgeIntf( NetworkingConfiguration netConfig, String intf )
        throws ArgonException
    {
        BridgeConfigurationManager.getInstance().setInternalBridgeIntf( netConfig, intf );        
    }
    
    /** Remove all interfaces bridged with the internal interface */
    public void disableInternalBridgeIntf( NetworkingConfiguration netConfig )
        throws ArgonException
    {
        BridgeConfigurationManager.getInstance().clearInternalBridgeIntf( netConfig );
    }

    public static final ArgonManagerImpl getInstance()
    {
        return INSTANCE;
    }

    private void pause()
    {
        try {
            Thread.sleep( 2000 );
        } catch ( Exception e ) {
            logger.warn( "Interrupted while pausing", e );
        }
    }

    static {
        InetAddress outside  = null;
        InetAddress inside = null;
        
        try {
            outside = InetAddress.getByName( BOGUS_OUTSIDE_ADDRESS_STRING );
            inside  = InetAddress.getByName( BOGUS_INSIDE_ADDRESS_STRING );
        } catch ( Exception e ) {
            System.err.println( "THIS SHOULD NEVER HAPPEN: error parsing " + BOGUS_OUTSIDE_ADDRESS_STRING );
            System.err.println( "THIS SHOULD NEVER HAPPEN: error parsing " + BOGUS_INSIDE_ADDRESS_STRING );
            System.err.println( "EXCEPTION: " + e );
            inside  = null;
            outside = null;
        }
        
        BOGUS_OUTSIDE_ADDRESS = outside;
        BOGUS_INSIDE_ADDRESS  = inside;
    }
}
