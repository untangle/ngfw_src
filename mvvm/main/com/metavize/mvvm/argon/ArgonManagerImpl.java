/*
 * Copyright (c) 2003, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: Argon.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.mvvm.argon;

import java.net.InetAddress;

import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

import org.apache.log4j.Logger;

import com.metavize.jnetcap.Netcap;
import com.metavize.jnetcap.InterfaceData;
import com.metavize.jnetcap.Shield;
import com.metavize.jnetcap.JNetcapException;

import com.metavize.mvvm.ArgonManager;
import com.metavize.mvvm.NetworkingConfiguration;

import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.firewall.InterfaceRedirect;

import com.metavize.mvvm.shield.ShieldNodeSettings;

public class ArgonManagerImpl implements ArgonManager
{    
    private static final Shield shield = Shield.getInstance();

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

    private boolean isShutdown = false;
        
    private ArgonManagerImpl()
    {
    }

    public void shieldStatus( InetAddress ip, int port )
    {
        if ( port < 0 || port > 0xFFFF ) {
            throw new IllegalArgumentException( "Invalid port: " + port );
        }
        shield.status( ip, port );
    }

    public void shieldReconfigure()
    {
        String shieldFile = Argon.getInstance().shieldFile;
        if ( shieldFile  != null ) {
            shield.config( shieldFile );
        }
    }

    synchronized public void updateAddress() throws ArgonException
    {
        if ( isShutdown ) {
            logger.warn( "MVVM is already shutting down, no longer able to update address" );
            return;
        }

        /* Update the rules */
        generateRules();

        IntfConverter intfConverter = IntfConverter.getInstance();

        String inside  = intfConverter.argonIntfToString( IntfConverter.INSIDE );
        String outside = intfConverter.argonIntfToString( IntfConverter.OUTSIDE );
            
        Netcap.updateAddress();
        
        try {
            this.insideIntfDataList = netcap.getInterfaceData( inside );
        } catch ( Exception e ) {
            logger.warn( "Exception retrieving inside interface data, setting to an empty list" );
            this.insideIntfDataList = EMPTY_INTF_DATA_LIST;
        }

        if ( this.insideIntfDataList == null ) this.insideIntfDataList = EMPTY_INTF_DATA_LIST;
        
        try {
            this.outsideIntfDataList = netcap.getInterfaceData( outside );
        } catch ( Exception e ) {
            logger.warn( "Exception retrieving inside interface data, setting to an empty list" );
            this.outsideIntfDataList = EMPTY_INTF_DATA_LIST;
        }
        
        if ( this.outsideIntfDataList == null ) this.outsideIntfDataList = EMPTY_INTF_DATA_LIST;
                
        IPMatcher.setInsideAddress( getInsideAddress(), getInsideNetmask());
        InetAddress outsideAddress = getOutsideAddress();
        InetAddress outsideNetmask = getOutsideNetmask();
        
        if (( outsideAddress == null ) || ( outsideAddress.equals( BOGUS_OUTSIDE_ADDRESS ))) {
            IPMatcher.setOutsideAddress((InetAddress)null, (InetAddress)null );
        } else {
            IPMatcher.setOutsideAddress( outsideAddress, outsideNetmask );
        }
    }

    /**
     * Load a networking configuration
     */
    synchronized public void loadNetworkingConfiguration( NetworkingConfiguration netConfig ) 
        throws ArgonException
    {
        BridgeConfigurationManager.getInstance().reconfigureBridge( netConfig );
        pause();
        updateAddress();
    }
    
    /* Indicate that the shutdown process has started, this is used to prevent NAT from both
     * re-enabling the bridge during shutdown, Argon will do that automatically. */
    synchronized void isShutdown() 
    {
        isShutdown = true;
    }    

    synchronized public void destroyBridge( NetworkingConfiguration netConfig, InetAddress internalAddress, 
                                            InetAddress internalNetmask ) throws ArgonException
    {
        if ( isShutdown ) {
            logger.warn( "MVVM is already shutting down, no longer able to destroy bridge" );
            return;
        }

        BridgeConfigurationManager.getInstance().destroyBridge( netConfig, 
                                                                internalAddress, internalNetmask );
        pause();
        updateAddress();
    }
    
    synchronized public void restoreBridge( NetworkingConfiguration netConfig ) throws ArgonException
    {
        if ( isShutdown ) {
            logger.warn( "MVVM is already shutting down, no longer able to restore bridge" );
            return;
        }

        BridgeConfigurationManager.getInstance().restoreBridge( netConfig );
        pause();
        updateAddress();
    }

    /* This re-enables the bridge, but checks if it is already enabled before attempting to 
     * re-enable it */
    synchronized public void argonRestoreBridge( NetworkingConfiguration netConfig ) throws ArgonException
    {
        BridgeConfigurationManager.getInstance().argonRestoreBridge( netConfig );
        pause();
        updateAddress();
    }

    public void disableLocalAntisubscribe()
    {
        RuleManager.getInstance().subscribeLocalOutside( true );
    }

    public void enableLocalAntisubscribe()
    {
        RuleManager.getInstance().subscribeLocalOutside( false );
    }

    public void disableDhcpForwarding()
    {
        RuleManager.getInstance().dhcpEnableForwarding( false );
    }

    public void enableDhcpForwarding()
    {
        RuleManager.getInstance().dhcpEnableForwarding( true );
    }

    /* Update all of the iptables rules and the inside address database */
    public void generateRules() throws ArgonException
    {
        RuleManager.getInstance().generateIptablesRules();
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

    public InetAddress getOutsideAddress()
    {
        if ( this.outsideIntfDataList.size() < 1 ) return null;
        return outsideIntfDataList.get( 0 ).getAddress();
    }

    public InetAddress getOutsideNetmask()
    {
        if ( this.outsideIntfDataList.size() < 1 ) return null;
        return outsideIntfDataList.get( 0 ).getNetmask();
    }

    public List<InterfaceData> getOutsideAliasList()
    {
        if ( this.outsideIntfDataList.size() < 2 ) return EMPTY_INTF_DATA_LIST;
        return Collections.unmodifiableList( outsideIntfDataList.subList( 1, outsideIntfDataList.size()));
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
            if ( settings == null ) {
                logger.error( "NULL Settings in list\n" );
                continue;
            }

            if ( !settings.isLive()) {
                logger.debug( "Ignoring disabled settings" );
                continue;
            }

            if ( settings.getAddress().isEmpty() || settings.getNetmask().isEmpty()) {
                logger.error( "Settings with empty address or netmask, ignoring" );
                continue;
            }
            
            logger.debug( "Adding shield node setting " + settings.getAddress().toString() + "/" + 
                          settings.getNetmask().toString() + " divider: " + settings.getDivider());
                        
            settingsList.add( new com.metavize.jnetcap.ShieldNodeSettings( settings.getDivider(), 
                                                                           settings.getAddress().getAddr(), 
                                                                           settings.getNetmask().getAddr()));
        }

        try {
            Shield.getInstance().setNodeSettings( settingsList );
        } catch ( Exception e ) {
            throw new ArgonException( "Unable to set the shield node settingss", e );
        }
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
