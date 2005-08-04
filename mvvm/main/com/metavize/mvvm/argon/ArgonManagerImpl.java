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

import org.apache.log4j.Logger;

import com.metavize.jnetcap.Netcap;
import com.metavize.jnetcap.Shield;

import com.metavize.mvvm.ArgonManager;
import com.metavize.mvvm.NetworkingConfiguration;

import com.metavize.mvvm.tran.firewall.IPMatcher;

public class ArgonManagerImpl implements ArgonManager
{    
    private static final Shield shield = Shield.getInstance();

    private static final ArgonManagerImpl INSTANCE = new ArgonManagerImpl();
    private static final String PUMP_FLAG = "pump";

    private final Logger logger = Logger.getLogger( ArgonManagerImpl.class );

    private final Netcap netcap = Netcap.getInstance();

    private final String BUNNICULA_BASE = System.getProperty( "bunnicula.home" );
    private final String BRIDGE_DISABLE_SCRIPT = BUNNICULA_BASE + "/networking/bridge-disable";
    private final String BRIDGE_ENABLE_SCRIPT  = BUNNICULA_BASE + "/networking/bridge-enable";

    private InetAddress insideAddress  = null;
    private InetAddress insideNetmask  = null;

    private InetAddress outsideAddress = null;
    private InetAddress outsideNetmask = null;

    private boolean isBridgeEnabled = true;
    private InetAddress natInsideAddress = null;
    private InetAddress natInsideNetmask = null;

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
        if ( Argon.shieldFile != null ) {
            shield.config( Argon.shieldFile );
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

        String inside  = intfConverter.addressString( IntfConverter.INSIDE );
        String outside = intfConverter.addressString( IntfConverter.OUTSIDE );
            
        Netcap.updateAddress();
        try {
            this.insideAddress  = netcap.getInterfaceAddress( inside );
            this.insideNetmask  = netcap.getInterfaceNetmask( inside );
            this.outsideAddress = netcap.getInterfaceAddress( outside );
            this.outsideNetmask = netcap.getInterfaceNetmask( outside );

            IPMatcher.setInsideAddress( insideAddress );
            IPMatcher.setOutsideAddress( outsideAddress );
        } catch ( Exception e ) {
            throw new ArgonException( "Unable to update the address", e );
        }
    }

    /**
     * Load a networking configuration
     */
    synchronized public void loadNetworkingConfiguration( NetworkingConfiguration netConfig ) 
        throws ArgonException
    {
        if ( isBridgeEnabled ) {
            restoreBridge( netConfig );
        } else {
            if ( !natInsideAddress.equals( insideAddress )) {
                logger.error( "Nat inside address(" + natInsideAddress + ") does not equal current " + 
                              "inside address(" + natInsideAddress + ")" );
            }

            if ( !natInsideNetmask.equals( insideNetmask )) {
                logger.error( "Nat inside netmask(" + natInsideNetmask + ") does not equal current " + 
                              "inside netmask(" + natInsideNetmask + ")" );
            }

            destroyBridge( netConfig, this.natInsideAddress, this.natInsideNetmask );
        }
    }

    /* Break down the bridge, (Only useful for NAT)
     * This will automatically update the iptables rules
     */
    synchronized public void destroyBridge( NetworkingConfiguration netConfig, InetAddress insideAddress, 
                                            InetAddress insideNetmask ) 
        throws ArgonException
    {
        if ( isShutdown ) {
            logger.warn( "MVVM is already shutting down, no longer able to destroy bridge" );
            return;
        }

        try {
            IntfConverter intfConverter = IntfConverter.getInstance();
            
            String inside  = intfConverter.addressString( IntfConverter.INSIDE );
            String outside = intfConverter.addressString( IntfConverter.OUTSIDE );
            
            String args;

            args  = " br0 ";
            args += inside  + " " + insideAddress.getHostAddress() + " " + 
                insideNetmask.getHostAddress() + " ";

            if ( netConfig.isDhcpEnabled()) {
                args += outside + " " + PUMP_FLAG;
            } else {
                String outsideAddress = netConfig.host().getAddr().getHostAddress();
                String outsideNetmask = netConfig.netmask().getAddr().getHostAddress();
                String gateway        = netConfig.gateway().getAddr().getHostAddress();
                
                args += outside  + " " + outsideAddress + " " + outsideNetmask + " " + gateway;
            }
            
            /* Call the rule generator */
            Process p = Runtime.getRuntime().exec( "sh " + BRIDGE_DISABLE_SCRIPT + args );
            
            if ( p.waitFor() != 0 ) {
                throw new ArgonException( "Error while destroying bridge" );
            }

            /* Dust settling */
            try {
                Thread.sleep( 2000 );
            } catch ( Exception e ) {
            }

            /* Generate new rules and then update the address database */
            updateAddress();

            this.isBridgeEnabled = false;
            this.natInsideAddress = insideAddress;
            this.natInsideNetmask = insideNetmask;
        } catch ( Exception e ) {
            logger.error( "Error while destroying the bridge", e );
            throw new ArgonException( "Unable to destroy bridge", e );
        }        
    }
    
    /* Restore the bridge and shutdown at the same time */
    synchronized void isShutdown() 
    {
        isShutdown = true;
    }
    
    /* XXXXXXXXXXXXXX Bad idea, basically a quick hack for argon to be the only package
     * that can restore the bridge after a shutdown */
    synchronized void argonRestoreBridge( NetworkingConfiguration netConfig ) throws ArgonException
    {
        /* Nothing to do, bridge was never modified */
        if ( this.isBridgeEnabled ) {
            logger.debug( "Bridge was already enabled, ignoring" );
            return;
        }

        try {
            IntfConverter intfConverter = IntfConverter.getInstance();
            
            String inside  = intfConverter.addressString( IntfConverter.INSIDE );
            String outside = intfConverter.addressString( IntfConverter.OUTSIDE );
            
            String args;

            args  = " br0 " + inside + " " + outside + " ";
            
            if ( netConfig.isDhcpEnabled()) {
                args += PUMP_FLAG;
            } else {
                String outsideAddress = netConfig.host().getAddr().getHostAddress();
                String outsideNetmask = netConfig.netmask().getAddr().getHostAddress();
                String gateway        = netConfig.gateway().getAddr().getHostAddress();
                
                args += " " + outsideAddress + " " + outsideNetmask + " " + gateway;
            }
            
            /* Call the rule generator */
            Process p = Runtime.getRuntime().exec( "sh " + BRIDGE_ENABLE_SCRIPT + args );
            
            if ( p.waitFor() != 0 ) {
                throw new ArgonException( "Error while restoring bridge" );
            }

            /* Dust settling */
            try {
                Thread.sleep( 2000 );
            } catch ( Exception e ) {
            }

            /* Generate new rules and then update the address database */
            updateAddress();

            isBridgeEnabled = true;
            this.natInsideAddress = null;
            this.natInsideNetmask = null;
        } catch ( Exception e ) {
            logger.error( "Error while restoring the bridge", e );
            throw new ArgonException( "Error restoring bridge", e );
        }        
    }

    
    /* Restore the bridge, (only useful for NAT)
     * This will automatically update the iptables rules
     */
    synchronized public void restoreBridge( NetworkingConfiguration netConfig ) throws ArgonException
    {
        if ( isShutdown ) {
            logger.warn( "MVVM is already shutting down, no longer able to restore bridge" );
            return;
        }
        
        try {
            IntfConverter intfConverter = IntfConverter.getInstance();
            
            String inside  = intfConverter.addressString( IntfConverter.INSIDE );
            String outside = intfConverter.addressString( IntfConverter.OUTSIDE );
            
            String args;

            args  = " br0 " + inside + " " + outside + " ";
            
            if ( netConfig.isDhcpEnabled()) {
                args += PUMP_FLAG;
            } else {
                String outsideAddress = netConfig.host().getAddr().getHostAddress();
                String outsideNetmask = netConfig.netmask().getAddr().getHostAddress();
                String gateway        = netConfig.gateway().getAddr().getHostAddress();
                
                args += " " + outsideAddress + " " + outsideNetmask + " " + gateway;
            }
            
            /* Call the rule generator */
            Process p = Runtime.getRuntime().exec( "sh " + BRIDGE_ENABLE_SCRIPT + args );
            
            if ( p.waitFor() != 0 ) {
                throw new ArgonException( "Error while restoring bridge" );
            }

            /* Dust settling */
            try {
                Thread.sleep( 2000 );
            } catch ( Exception e ) {
            }

            /* Generate new rules and then update the address database */
            updateAddress();

            isBridgeEnabled = true;
            this.natInsideAddress = null;
            this.natInsideNetmask = null;
        } catch ( Exception e ) {
            logger.error( "Error while restoring the bridge", e );
            throw new ArgonException( "Error restoring bridge", e );
        }        
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

    public InetAddress getInsideAddress()
    {
        return this.insideAddress;
    }

    public InetAddress getInsideNetmask()
    {
        return this.insideNetmask;
    }

    public InetAddress getOutsideAddress()
    {
        return this.outsideAddress;
    }

    public InetAddress getOutsideNetmask()
    {
        return this.outsideNetmask;
    }

    public static final ArgonManagerImpl getInstance()
    {
        return INSTANCE;
    }
}
