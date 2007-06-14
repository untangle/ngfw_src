/*
 * $HeadURL:$
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
package com.untangle.node.router;

import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ArgonManager;

import com.untangle.uvm.networking.SetupState;
import com.untangle.uvm.networking.Interface;
import com.untangle.uvm.networking.NetworkSpace;
import com.untangle.uvm.networking.DhcpLeaseRule;
import com.untangle.uvm.networking.DnsStaticHostRule;
import com.untangle.uvm.networking.RedirectRule;
import com.untangle.uvm.networking.IPNetwork;
import com.untangle.uvm.networking.IPNetworkRule;
import com.untangle.uvm.networking.NetworkSpacesSettings;
import com.untangle.uvm.networking.NetworkSpace;
import com.untangle.uvm.networking.NetworkSpacesSettingsImpl;
import com.untangle.uvm.networking.ServicesSettings;
import com.untangle.uvm.networking.ServicesSettingsImpl;
import com.untangle.uvm.networking.EthernetMedia;
import com.untangle.uvm.networking.NetworkUtil;

import com.untangle.uvm.networking.internal.DhcpLeaseInternal;
import com.untangle.uvm.networking.internal.DnsStaticHostInternal;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.uvm.networking.internal.NetworkSpaceInternal;
import com.untangle.uvm.networking.internal.RedirectInternal;
import com.untangle.uvm.networking.internal.ServicesInternalSettings;

import com.untangle.uvm.security.Tid;

import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.ValidateException;

import com.untangle.uvm.node.firewall.ip.IPMatcher;
import com.untangle.uvm.node.firewall.ip.IPDBMatcher;
import com.untangle.uvm.node.firewall.ip.IPMatcherFactory;
import com.untangle.uvm.node.firewall.intf.IntfMatcher;
import com.untangle.uvm.node.firewall.intf.IntfMatcherFactory;
import com.untangle.uvm.node.firewall.port.PortMatcher;
import com.untangle.uvm.node.firewall.port.PortMatcherFactory;
import com.untangle.uvm.node.firewall.protocol.ProtocolMatcher;
import com.untangle.uvm.node.firewall.protocol.ProtocolMatcherFactory;

class SettingsManager
{
    private final Logger logger = Logger.getLogger( SettingsManager.class );

    SettingsManager()
    {
    }

    /* The network settings are the current settings.  The NetworkingManager always
     * returns a copy, so it is safe to mess around with this object */
    NetworkSpacesSettings toNetworkSettings( NetworkSpacesSettings networkSettings, 
                                             RouterBasicSettings routerSettings )
    {
        ((NetworkSpacesSettingsImpl)networkSettings).setSetupState( SetupState.BASIC );        
        List<NetworkSpace> networkSpaceList = networkSettings.getNetworkSpaceList();

        NetworkSpace primary;
        NetworkSpace natSpace;
        
        /* Assuming there is at least one network space */
        primary =networkSpaceList.get( 0 );
        
        /* Just ignore the previous settings */
        natSpace = new NetworkSpace();
        natSpace.setName( NetworkUtil.DEFAULT_SPACE_NAME_NAT );
        
        boolean isNatEnabled = routerSettings.getNatEnabled();
        natSpace.setLive( isNatEnabled );
        List<IPNetworkRule> networkList = new LinkedList<IPNetworkRule>();
            
        networkList.add( IPNetworkRule.makeInstance( routerSettings.getNatInternalAddress(),
                                                     routerSettings.getNatInternalSubnet()));
        natSpace.setNetworkList( networkList );
        natSpace.setIsDhcpEnabled( false );
        natSpace.setIsTrafficForwarded( true );
        natSpace.setIsNatEnabled( true );
        natSpace.setNatSpace( primary );
        natSpace.setNatAddress( null );
        
        /* DMZ is disabled on this space */
        natSpace.setIsDmzHostEnabled( false );
        natSpace.setIsDmzHostLoggingEnabled( false );
        natSpace.setDmzHost( null );
        
        /* DMZ settings are registered against the primary space */
        primary.setIsDmzHostEnabled( routerSettings.getDmzEnabled());
        primary.setIsDmzHostLoggingEnabled( routerSettings.getDmzLoggingEnabled());
        primary.setDmzHost( routerSettings.getDmzAddress());
        
        /* set the list of network spaces */
        networkSpaceList.clear();
        networkSpaceList.add( primary );
        networkSpaceList.add( natSpace );
        networkSettings.setNetworkSpaceList( networkSpaceList );

        /* The mtu is not configurable from this panel */
        primary.setMtu( primary.DEFAULT_MTU );
        natSpace.setMtu( natSpace.DEFAULT_MTU );

        /* Setup the interfaces */
        List<Interface> interfaceList = networkSettings.getInterfaceList();

        /* Move the internal interface into the secondary network space */
        boolean foundInternal = false;
        for ( Interface intf : interfaceList ) {
            intf.setNetworkSpace( primary );

            if ( intf.getArgonIntf() == IntfConstants.INTERNAL_INTF ) {
                foundInternal = true;
                if ( isNatEnabled ) intf.setNetworkSpace( natSpace );
            };
        }
        
        if ( !foundInternal ) {
            /* XXXX This is error code, but it should probably try to retain some of the interface
             * settings XXX */
            logger.error( "The interface list did not contain the internal " + 
                          "interface, creating a new interface list" );
            
            interfaceList = new LinkedList<Interface>();

            byte argonIntfArray[] = UvmContextFactory.context().localIntfManager().getArgonIntfArray();
                
            Arrays.sort( argonIntfArray );
            for ( byte argonIntf : argonIntfArray ) {
                /* The VPN interface doesn't belong to a network space */
                if ( argonIntf == IntfConstants.VPN_INTF ) continue;
                
                /* Add each interface to the list */
                Interface intf =  new Interface( argonIntf, EthernetMedia.AUTO_NEGOTIATE, true );
                intf.setName( IntfConstants.toName( argonIntf ));
                if ( isNatEnabled && ( argonIntf == IntfConstants.INTERNAL_INTF )) {
                    intf.setNetworkSpace( natSpace );
                } else {
                    intf.setNetworkSpace( primary );
                }
                
                interfaceList.add( intf );
            }
        }

        networkSettings.setInterfaceList( interfaceList );

        /* Set the redirects */
        networkSettings.setRedirectList( routerSettings.getRedirectList());
        
        return networkSettings;
    }

    NetworkSpacesSettings toNetworkSettings( NetworkSpacesSettings networkSettings, 
                                             RouterAdvancedSettings advanced )
        throws ValidateException
    {

        logger.debug( "New advanced settings: " + advanced );

        ((NetworkSpacesSettingsImpl)networkSettings).setSetupState( SetupState.ADVANCED );
        /* Is enabled should have already been set */
        // networkSettings.setIEnabled();
        
        /* Fix all of the links to NetworkSpaces */
        List<NetworkSpace> networkSpaceList = advanced.getNetworkSpaceList();

        /* Replace the primary network space */
        NetworkSpace primary = networkSpaceList.get( 0 );

        NetworkSpace previousPrimary = networkSettings.getNetworkSpaceList().get( 0 );
        
        /* MTU is the only value that carries over */
        previousPrimary.setMtu( primary.getMtu());
        previousPrimary.setName( primary.getName());
        previousPrimary.setIsTrafficForwarded( primary.getIsTrafficForwarded());

        /* Swap out the primary network space */
        networkSpaceList.remove( 0 );
        networkSpaceList.add( 0, previousPrimary );
        
        Map<Long,NetworkSpace> networkSpaceMap = new HashMap<Long,NetworkSpace>();

        for( NetworkSpace space : networkSpaceList ) networkSpaceMap.put( space.getBusinessPapers(), space );

        List<Interface> interfaceList = advanced.getInterfaceList();

        boolean hasExternalIntf = false;

        for( Interface intf : interfaceList ) {
            String name = intf.getName();

            if ( intf.getArgonIntf() == IntfConstants.EXTERNAL_INTF ) {
                intf.setNetworkSpace( previousPrimary );
                hasExternalIntf = true;
                continue;
            }

            if ( intf.getNetworkSpace() == null ) {
                throw new ValidateException( "Interface " + name + " has an empty network space" );
            }
            
            NetworkSpace space = networkSpaceMap.get( intf.getNetworkSpace().getBusinessPapers());
            /* This shouldn't happen */
            if ( space == null ) {
                throw new ValidateException( "Interface " + name + " is not assigned a network space" );
            }
            
            logger.debug( "stitching the interface to " + space.hashCode() +  " " + space.getBusinessPapers());
            intf.setNetworkSpace( space );
            space = intf.getNetworkSpace();
            logger.debug( "interface -> " + space.hashCode() +  " " + space.getBusinessPapers());
        }

        if ( !hasExternalIntf ) {
            /* Depending on the contract with the GUI, this may or may not be true */
            logger.debug( "Interface list doesn't contain external interface, inserting" );
            for ( Interface intf : networkSettings.getInterfaceList()) {
                if ( intf.getArgonIntf() != IntfConstants.EXTERNAL_INTF ) continue;
                intf.setNetworkSpace( previousPrimary );
                interfaceList.add( 0, intf );
                hasExternalIntf = true;
                break;
            }
            
            /* If the previous list didn't have an external interface, then add one */
            if ( !hasExternalIntf ) {
                logger.warn( "Previous list didn't have external interface, inserting" );
                Interface intf = 
                    new Interface( IntfConstants.EXTERNAL_INTF, EthernetMedia.AUTO_NEGOTIATE, true );
                intf.setNetworkSpace( previousPrimary );
                interfaceList.add( 0, intf );
            }
        }


        for ( NetworkSpace space : networkSpaceList ) {
            NetworkSpace natSpace = space.getNatSpace();
            if ( natSpace != null ) {
                natSpace = networkSpaceMap.get( natSpace.getBusinessPapers());
                /* if this happens there is nothing the user can do. */
                if ( natSpace == null ) {
                    throw new ValidateException( "Network space '" + space.getName() + 
                                                 "' has an invalid nat sace." );
                }
                space.setNatSpace( natSpace );
            } else if ( !space.getIsPrimary() && space.isLive() && space.getIsNatEnabled()) {
                logger.warn( "Network space: " + space.getName() + " has a null nat space" );
            }
        }
        
        networkSettings.setInterfaceList( interfaceList );

        for( Interface intf : interfaceList ) {
            NetworkSpace space = intf.getNetworkSpace();
            logger.debug( "interface -> " + space.hashCode() +  " " + space.getBusinessPapers());
        }

        networkSettings.setNetworkSpaceList( networkSpaceList );
        networkSettings.setRoutingTable( advanced.getRoutingTable());
        networkSettings.setRedirectList( advanced.getRedirectList());
        
        /* Everything else is not configurable here. */
        return networkSettings;
    }


    /* This removes the external interface, since it cannot be modified */
    RouterAdvancedSettings toAdvancedSettings( NetworkSpacesSettings network,
                                            NetworkSpacesInternalSettings networkInternal,
                                            ServicesInternalSettings services )
    {
        network.getNetworkSpaceList().get( 0 ).setIsPrimary( true );
        List<Interface> interfaceList = network.getInterfaceList();
        
        for ( Iterator<Interface> iter = interfaceList.iterator() ; iter.hasNext() ; ) {
            Interface intf = iter.next();
            if ( intf.getArgonIntf() == IntfConstants.EXTERNAL_INTF ) iter.remove();
        }

        NetworkSpaceInternal primarySpace = networkInternal.getNetworkSpaceList().get( 0 );

        List<IPDBMatcher> localMatcherList = getLocalMatcherList( primarySpace );

        return new RouterAdvancedSettingsImpl( network, services.toSettings(), localMatcherList );
    }

    RouterBasicSettings toBasicSettings( Tid tid,
                                      NetworkSpacesInternalSettings networkSettings,
                                      ServicesInternalSettings servicesSettings )
    {                
        List<NetworkSpaceInternal> networkSpaceList = networkSettings.getNetworkSpaceList();

        NetworkSpaceInternal primarySpace =
            ( networkSpaceList.size() > 0 ) ? networkSpaceList.get( 0 ) : null;

        List<IPDBMatcher> localMatcherList = getLocalMatcherList( primarySpace );
        
        RouterBasicSettings routerSettings = new RouterSettingsImpl( tid, SetupState.BASIC, localMatcherList );
        
        /* Get the network space list in order to determine how many spaces there are */
        if ( networkSpaceList.size() == 1 ) {
            /* Use this for the dmz */
            NetworkSpaceInternal networkSpace = networkSpaceList.get( 0 );
            
            /* Nat is disabled */
            routerSettings.setNatEnabled( false );
            routerSettings.setNatInternalAddress( RouterUtil.DEFAULT_NAT_ADDRESS );
            routerSettings.setNatInternalSubnet( RouterUtil.DEFAULT_NAT_NETMASK );
        } else if ( networkSpaceList.size() > 1 ) {
            NetworkSpaceInternal networkSpace = networkSpaceList.get( 1 );
            
            routerSettings.setNatEnabled( networkSpace.getIsEnabled());
            IPNetwork primary = networkSpace.getPrimaryAddress();
            routerSettings.setNatInternalAddress( primary.getNetwork());
            routerSettings.setNatInternalSubnet( primary.getNetmask());
        } else {
            logger.error( "No network spaces, returning default settings" );
            routerSettings = getDefaultSettings( tid );
        }
        
        /* DMZ settings (DMZ settings are registered against the primary space) */
        setupDmz( routerSettings, networkSpaceList.get( 0 ));

        /* Setup the services settings */
        
        /* dhcp settings */
        if ( servicesSettings == null ) {
            /* Default services settings */
            routerSettings.setDhcpEnabled( true );
            routerSettings.setDhcpStartAddress( RouterUtil.DEFAULT_DHCP_START );
            routerSettings.setDhcpEndAddress( RouterUtil.DEFAULT_DHCP_END );
            routerSettings.setDhcpLeaseTime( RouterUtil.DEFAULT_LEASE_TIME_SEC );
            routerSettings.setDhcpLeaseList( new LinkedList<DhcpLeaseRule>());
            
            routerSettings.setDnsEnabled( true );
            routerSettings.setDnsLocalDomain( null );
            routerSettings.setDnsStaticHostList( new LinkedList<DnsStaticHostRule>());
        } else {
            routerSettings.setDhcpEnabled( servicesSettings.getIsDhcpEnabled());
            routerSettings.setDhcpStartAddress( servicesSettings.getDhcpStartAddress());
            routerSettings.setDhcpEndAddress( servicesSettings.getDhcpEndAddress());
            routerSettings.setDhcpLeaseTime( servicesSettings.getDhcpLeaseTime());
            routerSettings.setDhcpLeaseList( servicesSettings.getDhcpLeaseRuleList());
            
            /* dns settings */
            routerSettings.setDnsEnabled( servicesSettings.getIsDnsEnabled());
            routerSettings.setDnsLocalDomain( servicesSettings.getDnsLocalDomain());
            routerSettings.setDnsStaticHostList( servicesSettings.getDnsStaticHostRuleList());
        }
        
        /* Setup the redirect settings */
        routerSettings.setRedirectList( networkSettings.getRedirectRuleList());

        /* Set the basic network settings for validation */
        /* XXX The network settings are already known, there isn't really a need to craete
         * a new object, but this is more convenient */
        routerSettings.
            setNetworkSettings( UvmContextFactory.context().networkManager().getBasicSettings());
                
        return routerSettings;
    }

    /** Convert a set of network settings from basic mode to advanced mode */
    /* This recycles network, but that is okay since the network manager always *
     * returns a copy */
    NetworkSpacesSettings basicToAdvanced( NetworkSpacesSettingsImpl ns )
    {
        /* Update the setup state */
        ns.setSetupState( SetupState.ADVANCED );
        
        /* use the interface out, because the interface has generics and the impl doesn't */
        NetworkSpacesSettings network = ns;

        NetworkSpace primary = (NetworkSpace)ns.getNetworkSpaceList().get( 0 );
        
        if ( primary.getIsDmzHostEnabled() && ( primary.getNetworkList().size() > 0 )) {
            
            IPNetworkRule primaryNetwork = (IPNetworkRule)primary.getNetworkList().get( 0 );
            IPaddr local = primaryNetwork.getNetwork();
            
            IntfMatcherFactory imf  = IntfMatcherFactory.getInstance();
            IPMatcherFactory   ipmf = IPMatcherFactory.getInstance();
            PortMatcherFactory pmf  = PortMatcherFactory.getInstance();
            ProtocolMatcherFactory prmf = ProtocolMatcherFactory.getInstance();
            
            RedirectRule dmz = new RedirectRule( true, prmf.getAllMatcher(),
                                                 imf.getExternalMatcher(), imf.getAllMatcher(),
                                                 ipmf.getAllMatcher(), ipmf.getLocalMatcher(),
                                                 pmf.getAllMatcher(), pmf.getAllMatcher(),
                                                 true, primary.getDmzHost(), -1 );

            /* Add the dmz to the end of the redirect list */
            network.getRedirectList().add( dmz );
        }
        
        /* Disable dmz host, it may/may not have been replaced by a redirect rule */
        primary.setIsDmzHostEnabled( false );
        
        return network;
    }

    /** Convert a set of network settings from basic mode to advanced mode */
    /* This recycles network, but that is okay since the network manager always *
     * returns a copy */
    NetworkSpacesSettings resetToBasic( Tid tid, NetworkSpacesSettingsImpl ns )
    {
        /* This object gets recycled so these values have to be saved beforehand */
        List<RedirectRule> redirectList = new LinkedList<RedirectRule>( ns.getRedirectList());
        
        /* Reset to the default settings */
        NetworkSpacesSettings settings = toNetworkSettings( ns, getDefaultSettings( tid ));
        
        /* Save the list of redirects */
        settings.setRedirectList( redirectList );
        
        return settings;

    }

    private void setupDmz( RouterBasicSettings settings, NetworkSpaceInternal space )
    {
        settings.setDmzEnabled( space.getIsDmzHostEnabled());
        settings.setDmzLoggingEnabled( space.getIsDmzHostLoggingEnabled());
        IPaddr dmz = space.getDmzHost();
        if ( dmz == null || dmz.isEmpty()) {
            /* Disable dmz, just so that it will save even if
             * it isn't in the corect network. */
            settings.setDmzEnabled( false );
            settings.setDmzAddress( RouterUtil.DEFAULT_DMZ_ADDRESS );
        } else {
            settings.setDmzAddress( dmz );
        }
    }

    private List<IPDBMatcher> getLocalMatcherList( NetworkSpaceInternal primary )
    {
        List<IPDBMatcher> list = new LinkedList<IPDBMatcher>();

        IPMatcherFactory ipmf = IPMatcherFactory.getInstance();
        
        list.addAll( RouterUtil.getInstance().getEmptyLocalMatcherList());
        
        /* If the primary space is not null, add a matcher for each alias */
        if ( primary != null ) {
            IPNetwork primaryNetwork = primary.getPrimaryAddress();
            
            for ( IPNetwork network : primary.getNetworkList()) {
                /* Don't add the primary network to the list, it is covered by local */
                if ( primaryNetwork != null && primaryNetwork.equals( network )) continue;

                /* Only add unicast address */
                if ( !network.isUnicast()) continue;

                list.add( ipmf.makeSingleMatcher( network.getNetwork()));
            }
        }
        
        return list;
    }

    RouterBasicSettings getDefaultSettings( Tid tid )
    {
        logger.info( "Using default settings" );

        RouterSettingsImpl settings = new RouterSettingsImpl( tid, SetupState.BASIC, 
                                                        RouterUtil.getInstance().getEmptyLocalMatcherList());

        List<RedirectRule> redirectList = new LinkedList<RedirectRule>();

        IntfMatcherFactory imf = IntfMatcherFactory.getInstance();
        IPMatcherFactory ipmf  = IPMatcherFactory.getInstance();
        PortMatcherFactory pmf = PortMatcherFactory.getInstance();
        ProtocolMatcherFactory  prmf = ProtocolMatcherFactory.getInstance();

        try {
            settings.setNatEnabled( true );
            settings.setNatInternalAddress( RouterUtil.DEFAULT_NAT_ADDRESS );
            settings.setNatInternalSubnet( RouterUtil.DEFAULT_NAT_NETMASK );

            settings.setDmzLoggingEnabled( false );

            /* DMZ Settings */
            settings.setDmzEnabled( false );
            /* A sample DMZ */
            settings.setDmzAddress( RouterUtil.DEFAULT_DMZ_ADDRESS );
            
            // A few port forwards
            // 21 to 21
            RedirectRule tmp = new RedirectRule( false, prmf.getTCPMatcher(),
                                                 imf.getExternalMatcher(), imf.getAllMatcher(),
                                                 ipmf.getAllMatcher(), ipmf.getLocalMatcher(),
                                                 pmf.getAllMatcher(), pmf.makeSingleMatcher( 21 ),
                                                 true, IPaddr.parse( "192.168.1.16" ), 21 );
            tmp.setDescription( "Forward incoming FTP traffic to 192.168.1.16" );
            redirectList.add( tmp );

            // 25 to 25
            tmp = new RedirectRule( false, prmf.getTCPMatcher(),
                                    imf.getExternalMatcher(), imf.getAllMatcher(),
                                    ipmf.getAllMatcher(), ipmf.getLocalMatcher(),
                                    pmf.getAllMatcher(), pmf.makeSingleMatcher( 25 ),
                                    true, IPaddr.parse( "192.168.1.16" ), 25 );
            tmp.setDescription( "Forward incoming mail server(SMTP) traffic to 192.168.1.16" );
            redirectList.add( tmp );
            
            // 80 to 80
            tmp = new RedirectRule( false, prmf.getTCPMatcher(),
                                    imf.getExternalMatcher(), imf.getAllMatcher(),
                                    ipmf.getAllMatcher(), ipmf.getLocalMatcher(),
                                    pmf.getAllMatcher(), pmf.makeSingleMatcher( 80 ),
                                    true, IPaddr.parse( "192.168.1.16" ), 80 );
            tmp.setDescription( "Forward incoming web/HTTP traffic (port 80) to 192.168.1.16" );
            redirectList.add( tmp );

            // 443 to 443
            tmp = new RedirectRule( false, prmf.getTCPMatcher(),
                                    imf.getExternalMatcher(), imf.getAllMatcher(),
                                    ipmf.getAllMatcher(), ipmf.getLocalMatcher(),
                                    pmf.getAllMatcher(), pmf.makeSingleMatcher( 443 ),
                                    true, IPaddr.parse( "192.168.1.16" ), 443 );
            tmp.setDescription( "Forward incoming secure web/HTTPS traffic to 192.168.1.16" );
            redirectList.add( tmp );
            
            // 8080 to 80
            tmp = new RedirectRule( false, prmf.getTCPMatcher(),
                                    imf.getExternalMatcher(), imf.getAllMatcher(),
                                    ipmf.getAllMatcher(), ipmf.getLocalMatcher(),
                                    pmf.getAllMatcher(), pmf.makeSingleMatcher( 8080 ),
                                    true, IPaddr.parse( "192.168.1.16" ), 80 );
            tmp.setDescription( "Redirect incoming traffic to port 8080 to port 80 on 192.168.1.16" );
            redirectList.add( tmp );

            
            for ( RedirectRule redirect : redirectList ) redirect.setLocalRedirect( true );

            tmp = new RedirectRule( false, prmf.getTCPAndUDPMatcher(),
                                    imf.getExternalMatcher(), imf.getAllMatcher(),
                                    ipmf.getAllMatcher(), ipmf.getAllMatcher(),
                                    pmf.getAllMatcher(), pmf.makeRangeMatcher( 6000, 10000 ),
                                    true, (IPaddr)null, 6000 );
            tmp.setDescription( "Redirect incoming traffic from ports 6000-10000 to port 6000" );
            redirectList.add( tmp );

            tmp = new RedirectRule( false, prmf.getTCPAndUDPMatcher(),
                                    imf.getInternalMatcher(), imf.getAllMatcher(),
                                    ipmf.getAllMatcher(), ipmf.parse( "1.2.3.4" ),
                                    pmf.getAllMatcher(), pmf.getAllMatcher(),
                                    true, IPaddr.parse( "4.3.2.1" ), 0 );
            tmp.setDescription( "Redirect outgoing traffic going to 1.2.3.4 to 4.2.3.1, (port is unchanged)" );
            tmp.setLog( true );

            redirectList.add( tmp );

            for ( RedirectRule redirect : redirectList ) {
                redirect.setLog( true );
                redirect.setCategory( "[Sample]" );
            }

            /* Enable DNS and DHCP */
            settings.setDnsEnabled( true );
            settings.setDhcpEnabled( true );

            settings.setDhcpStartAndEndAddress( RouterUtil.DEFAULT_DHCP_START, RouterUtil.DEFAULT_DHCP_END );
        } catch ( Exception e ) {
            logger.error( "This should never happen", e );
        }

        settings.setRedirectList( redirectList );

        return settings;
    }
}
