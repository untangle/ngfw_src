/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.nat;

import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.IntfConstants;

import com.metavize.mvvm.security.Tid;

import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.IPaddr;

import com.metavize.mvvm.tran.firewall.ip.IPMatcher;
import com.metavize.mvvm.tran.firewall.ip.IPMatcherFactory;
import com.metavize.mvvm.tran.firewall.intf.IntfMatcher;
import com.metavize.mvvm.tran.firewall.intf.IntfMatcherFactory;
import com.metavize.mvvm.tran.firewall.port.PortMatcher;
import com.metavize.mvvm.tran.firewall.port.PortMatcherFactory;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;

import com.metavize.mvvm.networking.NetworkSettings;
import com.metavize.mvvm.networking.NetworkUtil;
import com.metavize.mvvm.networking.IPNetworkRule;
import com.metavize.mvvm.networking.IPNetwork;
import com.metavize.mvvm.networking.Route;
import com.metavize.mvvm.networking.Interface;
import com.metavize.mvvm.networking.NetworkSpace;

/**
 * Class to handle all of the different transformation on settings.
 * Since NetworkSpaces is the union of networking from the MVVM and DHCP, DNS and NAT in
 * a transform, the settings are not as simple as they are in just a single transform
 */
class SettingsManager
{
    private final Logger logger = Logger.getLogger( SettingsManager.class );

    SettingsManager()
    {
    }

    /**
     * The wrapper is really only used for the GUI.
     * This function takes you from the internal representation to the GUI representation
     */
    NatSettingsWrapper buildWrapper( NetworkSpacesSettingsPriv settingsPriv )
    {
        NatSettings nat = settingsPriv.getNatSettings();
        NetworkSettings networkSettings = settingsPriv.getNetworkSettings();

        NatSettingsWrapper wrapper = new NatSettingsWrapper( nat, networkSettings );

        SetupState setupState = settingsPriv.getSetupState();
        logger.debug( "Building settings for the " + setupState + " state." );
        if ( setupState.equals( SetupState.UNCONFIGURED )) {
            /* !!!!!! XXX Implement me */
        } else if ( setupState.equals( SetupState.NETWORK_SHARING )) {
            /* !!!!!! XXX Implement me */
        } else if ( setupState.equals( SetupState.BASIC )) {
            /* */
            List<NetworkSpace> networkSpaceList = networkSettings.getNetworkSpaceList();

            /* This is from a priv, so it can assume the size is at least 1, and that the
             * settings are all valid. */
            if ( networkSpaceList.size() > 1 ) {
                /* Get the second network space, that is the nat space */
                NetworkSpace natSpace = networkSpaceList.get( 1 );

                /* assume the nat internal address is the first address */
                IPNetworkRule network = (IPNetworkRule)natSpace.getNetworkList().get( 0 );
                
                nat.setIsNatEnabled( true );

                logger.debug( "Setting the nat address["+ network.getNetwork() +"] netmask["+ network.getNetmask() +"]" );
                nat.setNatAddress( network.getNetwork());
                nat.setNatNetmask( network.getNetmask());
                
                nat.setIsDmzHostEnabled( natSpace.getIsDmzHostEnabled());
                nat.setDmzHost( natSpace.getDmzHost());
                nat.setIsDmzLoggingEnabled( natSpace.getIsDmzLoggingEnabled());
            } else {
                
            }

        } else if ( setupState.equals( SetupState.ADVANCED )) {
            /* */
            
        } else {
            logger.error( "Settings are in an unknown state: " + setupState + " using unconfigured." );
        }

        return wrapper;
    }
    
    NatSettingsWrapper getDefaultSettings( Tid tid )
    {
        logger.info( "Using default settings" );
        
        NatSettings natSettings = new NatSettings( tid );
                
        try {
            /* Insert some sample redirects */
            addSampleRedirects( natSettings );
            
            /* Set the box into basic mode */
            natSettings.setSetupState( SetupState.BASIC );

            /* Configure NAT */
            natSettings.setIsNatEnabled( true );
            natSettings.setNatAddress( IPaddr.parse( "192.168.1.1" ));
            natSettings.setNatNetmask( IPaddr.parse( "255.255.255.0" ));
            
            /* Disable the DMZ host */
            natSettings.setIsDmzHostEnabled( false );
            natSettings.setDmzHost( IPaddr.parse( "192.168.1.2" ));
            natSettings.setIsDmzLoggingEnabled( false );
            
            /* Enable DNS and DHCP */
            natSettings.setDnsEnabled( true );
            natSettings.setDhcpEnabled( true );
            
            natSettings.setDhcpStartAndEndAddress( IPaddr.parse( "192.168.1.100" ),
                                                IPaddr.parse( "192.168.1.200" ));
        } catch ( Exception e ) {
            logger.error( "This should never happen", e );
        }

        NetworkSettings networkSettings = MvvmContextFactory.context().networkManager().getNetworkSettings();

        NatSettingsWrapper wrapper = new NatSettingsWrapper( natSettings, networkSettings );

        return wrapper;
    }

    private void addSampleRedirects( NatSettings natSettings ) throws Exception
    {
        IntfMatcherFactory imf = IntfMatcherFactory.getInstance();
        IPMatcherFactory ipmf   = IPMatcherFactory.getInstance();
        PortMatcherFactory pmf = PortMatcherFactory.getInstance();

        List<RedirectRule> redirectList = new LinkedList<RedirectRule>();
    
        /* Sample redirects */
        RedirectRule tmp = new RedirectRule( false, ProtocolMatcher.MATCHER_ALL,
                                             imf.getExternalMatcher(), imf.getAllMatcher(),
                                             ipmf.getAllMatcher(), ipmf.getAllMatcher(),
                                             pmf.getAllMatcher(), pmf.makeSingleMatcher( 8080 ),
                                             true, IPaddr.parse( "192.168.1.16" ), 80 );
        tmp.setDescription( "Redirect incoming traffic to EdgeGuard port 8080 to port 80" +
                            " on 192.168.1.16" );
        tmp.setLog( true );
        
        redirectList.add( tmp );
        
        tmp = new RedirectRule( false, ProtocolMatcher.MATCHER_ALL,
                                imf.getExternalMatcher(), imf.getAllMatcher(),
                                ipmf.getAllMatcher(), ipmf.getAllMatcher(),
                                pmf.getAllMatcher(), pmf.makeRangeMatcher( 6000, 10000 ),
                                true, (IPaddr)null, 6000 );
        tmp.setDescription( "Redirect incoming traffic from ports 6000-10000 to port 6000" );
        redirectList.add( tmp );

        tmp = new RedirectRule( false, ProtocolMatcher.MATCHER_ALL,
                                imf.getInternalMatcher(), imf.getAllMatcher(),
                                ipmf.getAllMatcher(), ipmf.parse( "1.2.3.4" ),
                                pmf.getAllMatcher(), pmf.getAllMatcher(),
                                true, IPaddr.parse( "4.3.2.1" ), 0 );
        tmp.setDescription( "Redirect outgoing traffic going to 1.2.3.4 to 4.2.3.1," +
                            " (port is unchanged)" );
        tmp.setLog( true );
        
        redirectList.add( tmp );
        
        for ( RedirectRule rule : redirectList ) rule.setCategory( "[Sample]" );

        natSettings.setRedirectList( redirectList );

    }

    void convertSetupState( NatSettingsWrapper wrapper ) throws TransformException
    {
        NatSettings natSettings = wrapper.getNatSettings();
        NetworkSettings networkSettings = wrapper.getNetworkSettings();
        
        SetupState setupState = natSettings.getSetupState();

        logger.debug( "Converting the settings from " + setupState );
                
        if ( setupState.equals( SetupState.UNCONFIGURED )) {
            /* !!!!!! XXX Implement me */
        } else if ( setupState.equals( SetupState.NETWORK_SHARING )) {
            /* !!!!!! XXX Implement me */
        } else if ( setupState.equals( SetupState.BASIC )) {
            /* */
            convertBasicSettings( natSettings, networkSettings );
        } else if ( setupState.equals( SetupState.ADVANCED )) {
            /* */
            
        } else {
            logger.error( "Settings are in an unknown state: " + setupState + " using unconfigured." );
        }
    }

    NetworkSpacesSettingsPriv makePrivSettings( NatSettingsWrapper wrapper )
        throws TransformException
    {
        return makePrivSettings( wrapper.getNatSettings(), wrapper.getNetworkSettings());
    }

    NetworkSpacesSettingsPriv makePrivSettings( NatSettings natSettings, NetworkSettings networkSettings )
        throws TransformException
    {
        logger.debug( "Creating private settings." );
        
        /* Retrieve a list of all of the spaces */
        List<NetworkSpace> networkSpaceList = networkSettings.getNetworkSpaceList();

        /* Build the private network settings */
        List<NetworkSpace> natdNetworkSpaceList = new LinkedList<NetworkSpace>();

        /* Create a list of all of the NATd spaces, this just makes it easier to
         * deal with down the line, */
        NetworkSpace serviceSpace = null;
                
        for ( NetworkSpace space : networkSpaceList ) {
            /* Determine the real NAT address */
            if ( space.getIsNatEnabled()) {
                natdNetworkSpaceList.add( space );
                
                NetworkSpace natSpace = space.getNatSpace();
                IPaddr natAddress = space.getNatAddress();

                if ( natSpace != null ) {
                    if ( natSpace.getIsDhcpEnabled()) {
                        natAddress = natSpace.getDhcpStatus().getAddress();
                    } else {
                        natAddress = natSpace.getPrimaryAddress().getNetwork();
                    }
                    
                    if ( natAddress == null ) natAddress = NetworkUtil.EMPTY_IPADDR;
                }

                space.setRealNatAddress( natAddress );
            }
        }

        /* Determine which space DHCP and DNS should run on, presently this is the
         * first space running NAT, or the first space that has an address if NAT is
         * not enabled, there must be at least one space that has an address */
        if ( natdNetworkSpaceList.size() > 0 ) {
            serviceSpace = natdNetworkSpaceList.get( 0 );
        } else {
            for ( NetworkSpace space : networkSpaceList ) {
                if ( space.hasPrimaryAddress()) serviceSpace = space;
            }
        }

        if ( serviceSpace == null ) {
            throw new TransformException( "Unable to determine which space to tun DHCP and DNS on." );
        }

        return new 
            NetworkSpacesSettingsPriv( natdNetworkSpaceList, serviceSpace, networkSettings, natSettings );
    }

    private void convertBasicSettings( NatSettings natSettings, NetworkSettings networkSettings) 
        throws TransformException
    {
        List<NetworkSpace> networkSpaceList = networkSettings.getNetworkSpaceList();
        
        if ( networkSpaceList.size() == 0 ) throw new TransformException( "Not enough network spaces" );
        
        if ( natSettings.getIsNatEnabled()) {
            NetworkSpace natSpace;
            NetworkSpace primary = networkSpaceList.get( 0 );
            if ( networkSpaceList.size() > 1 ) {
                /* Get the second network space, that is the nat space */
                natSpace = networkSpaceList.get( 1 );
            } else {
                logger.debug( "Creating a new network space for NAT." );
                natSpace = new NetworkSpace();
                networkSpaceList.add( natSpace );
            }

            /* Setup the internal address space, it is just one network. */
            IPNetworkRule internal = 
                IPNetworkRule.makeIPNetwork( natSettings.getNatAddress(), natSettings.getNatNetmask());

            /* Configure nat */
            List<IPNetworkRule> networkList = (List<IPNetworkRule>)natSpace.getNetworkList();
            networkList.clear();
            networkList.add( internal );            
            natSpace.setIsNatEnabled( true );
            natSpace.setNetworkList( networkList );

            Interface internalIntf = null;
            for ( Interface intf : (List<Interface>)networkSettings.getInterfaceList()) {
                if ( intf.getArgonIntf() == IntfConstants.INTERNAL_INTF ) {
                    internalIntf = intf;
                    /* Steal the interface and put it into the internal space */
                intf.setNetworkSpace( natSpace );
                break;
                }
            }
        
            if ( internalIntf == null ) {
                throw new TransformException( "Unable to find the internal interface" );
            }
            
            /* Configure the DMZ for this network space */
            natSpace.setIsDmzHostEnabled( natSettings.getIsDmzHostEnabled());
            natSpace.setDmzHost( natSettings.getDmzHost());
            natSpace.setIsDmzLoggingEnabled( natSettings.getIsDmzLoggingEnabled());
            
            /* DHCP must be disabled for this space */
            natSpace.setIsDhcpEnabled( false );
            natSpace.setIsTrafficForwarded( true );

            /* Set the space where this traffic gets NATd to */
            natSpace.setNatSpace( primary );
        } else {
            /* Nuke the second network space if it exists */
            if ( networkSpaceList.size() > 1 ) {
                logger.debug( "Deleting " + ( networkSpaceList.size() - 1 ) + " network space(s)." );

                if ( networkSpaceList.size() > 2 ) {
                    logger.warn( "There are currently more than two network spaces in a basic setup." );
                }

                /* !!! Also have to clear out all of the items, 
                 * backup the NAT settings, so they can be presented. */
                NetworkSpace primary = networkSpaceList.get( 0 );
                networkSpaceList.clear();
                networkSpaceList.add( primary );
            }

            NetworkSpace primary = networkSpaceList.get( 0 );
            
            /* Put all of the interfaces into the primary network space. */
            for ( Interface intf : (List<Interface>)networkSettings.getInterfaceList()) {
                intf.setNetworkSpace( primary );
            }
        }
    }

    /* Utility method to create a single network space that is running NAT */
    /** !!!! Delete this method, the basic setup means that this is no longer necessary */
    private void addNatSpace( NatSettingsWrapper wrapper ) throws Exception
    {
        NetworkSettings networkSettings = wrapper.getNetworkSettings();

        List<NetworkSpace> spaceList = networkSettings.getNetworkSpaceList();

        if ( spaceList.size() < 1 ) throw new Exception( "There are no network spaces." );

        NetworkSpace natSpace = null;
        if ( spaceList.size() < 2 ) {
            natSpace = new NetworkSpace();
            spaceList.add( natSpace );
        } else {
            natSpace = spaceList.get( 1 );
        }
        
        List<IPNetworkRule> networkList = (List<IPNetworkRule>)natSpace.getNetworkList();

        /* Delete all of the items in the list, and create a single new item */
        networkList.clear();
        IPNetworkRule internal = IPNetworkRule.parse( "192.168.2.1/24" );
        networkList.add( internal );
        
        /* ??? Not sure if this has to be set back */
        natSpace.setNetworkList( networkList );
        
        List<Interface> intfList = (List<Interface>)networkSettings.getInterfaceList();
        
        natSpace.setIsTrafficForwarded( true );
        natSpace.setIsDhcpEnabled( false );
        natSpace.setIsNatEnabled( true );
        /* !!!! Just for giggles right now */
        natSpace.setNatAddress( IPaddr.parse( "10.0.5.1" ));
        natSpace.setIsDmzHostEnabled( false );

        int index = 1;
        for ( NetworkSpace space : (List<NetworkSpace>)networkSettings.getNetworkSpaceList()) {
            logger.info( "Network space["+ index++ +"]: DHCP[" + space.getIsDhcpEnabled() + "]" );
            
            for ( IPNetworkRule rule : (List<IPNetworkRule>)space.getNetworkList()) {
                logger.info( "Network: " + rule.getIPNetwork());
            }
            
            logger.info( space.getDhcpStatus());
        }

        /* ??? Not sure if this has to be set back */
        networkSettings.setInterfaceList( intfList );

        wrapper.setSetupState( SetupState.BASIC );
        wrapper.setIsNatEnabled( true );
        wrapper.setNatAddress( internal.getNetwork());
        wrapper.setNatNetmask( internal.getNetmask());
        wrapper.setIsDmzHostEnabled( false );
    }    
}
