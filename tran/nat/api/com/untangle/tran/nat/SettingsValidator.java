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

package com.untangle.tran.nat;

import java.net.InetAddress;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.untangle.mvvm.InterfaceAlias;

import com.untangle.mvvm.tran.AddressValidator;
import com.untangle.mvvm.tran.ValidateException;

import com.untangle.mvvm.tran.firewall.MACAddress;

import com.untangle.mvvm.networking.BasicNetworkSettings;
import com.untangle.mvvm.networking.DhcpLeaseRule;
import com.untangle.mvvm.networking.IPNetworkRule;
import com.untangle.mvvm.networking.NetworkSpace;
import com.untangle.mvvm.networking.RedirectRule;


import com.untangle.mvvm.tran.IPaddr;

class SettingsValidator
{
    private static final SettingsValidator INSTANCE = new SettingsValidator();

    private SettingsValidator()
    {
    }

    /* Validation method */
    void validate( NatSettings natSettings ) throws ValidateException
    {
        boolean isStartAddressValid = true;
        boolean isEndAddressValid   = true;
        
        /* Update PING redirects */
        for ( RedirectRule rule : natSettings.getRedirectList()) rule.fixPing();

        boolean natEnabled = natSettings.getNatEnabled();
        IPaddr  natInternalAddress = natSettings.getNatInternalAddress();
        IPaddr  natInternalSubnet = natSettings.getNatInternalSubnet();

        AddressValidator av = AddressValidator.getInstance();

        if ( natEnabled ) {
            if ( natInternalAddress == null || natInternalSubnet == null  ||
                 natInternalAddress.isEmpty() || natInternalSubnet.isEmpty()) {
                throw new ValidateException( "Enablng NAT requires an \"Internal IP address\" and " +
                                             "an \"Internal Subnet\"" );
            }
            
            InetAddress address = natInternalAddress.getAddr();
            
            if ( av.isIllegalAddress( address )) {
                throw new ValidateException( "The \"Internal IP address\" is invalid" );
            }
        }
        
        if ( natSettings.getDmzEnabled()) {
            IPaddr dmzAddress = natSettings.getDmzAddress();
            if ( dmzAddress == null || dmzAddress.isEmpty()) {
                throw new ValidateException( "Enabling DMZ requires a target IP address" );
            }
            
            if ( natEnabled && !dmzAddress.isInNetwork( natInternalAddress, natInternalSubnet )) {
                throw new ValidateException( "When NAT is enabled, the \"DMZ address\" in the DMZ Host " +
                                             "panel must be in the internal network." );
            }
        }

        /* Validate the DHCP settings */
        IPaddr host = null;
        IPaddr netmask = null;

        List<InterfaceAlias> aliasList = new LinkedList<InterfaceAlias>();

        if ( natSettings.getNatEnabled()) {
            aliasList.add( new InterfaceAlias( natSettings.getNatInternalAddress(), 
                                               natSettings.getNatInternalSubnet()));
        } else {
            BasicNetworkSettings networkSettings = natSettings.getNetworkSettings();
            if ( networkSettings != null ) {
                aliasList.add( new InterfaceAlias( networkSettings.host(), networkSettings.netmask()));
                aliasList.addAll( networkSettings.getAliasList());
            }
        }

        validateDhcpSettings( natSettings, aliasList );
    }

    /* Validation method */
    void validate( NatAdvancedSettingsImpl natSettings ) throws ValidateException
    {
        List<InterfaceAlias> aliasList = new LinkedList<InterfaceAlias>();

        /* XXX The service space is either the first space, or the first space with NAT enabled.
         * this functionality is repeated inside of NetworkSpacesInternalSettings XXX */
        List<NetworkSpace> networkSpaceList = natSettings.getNetworkSpaceList();

        BasicNetworkSettings networkSettings = natSettings.getNetworkSettings();
        
        int index = 0;
        int c = 0;
        for ( NetworkSpace space : networkSpaceList ) {
            if ( space.isLive() && space.getIsNatEnabled()) {
                index = c;
                break;
            }
            c++;
        }

        if (( networkSpaceList.size() == 0 ) || ( index == 0 )) {
            if ( networkSettings != null ) {
                aliasList.add( new InterfaceAlias( networkSettings.host(), networkSettings.netmask()));
                aliasList.addAll( networkSettings.getAliasList());
            }
        } else {
            NetworkSpace space = networkSpaceList.get( index );
            for ( IPNetworkRule rule : (List<IPNetworkRule>)space.getNetworkList()) {
                aliasList.add( new InterfaceAlias( rule.getNetwork(), rule.getNetmask()));
            }
        }

        validateDhcpSettings( natSettings, aliasList );
    }

    private void validateDhcpSettings( NatCommonSettings natSettings, List<InterfaceAlias> aliasList )
        throws ValidateException
    {
        /* No need to validate the DHCP settings, they are not enabled */
        if ( !natSettings.getDhcpEnabled()) return;

        boolean hasNetwork = true;

        String networkString = "";

        if ( !aliasList.isEmpty()) {
            InterfaceAlias alias = aliasList.get( 0 );
            networkString = alias.getAddress().toString() + "/" + alias.getNetmask().toString();

            if ( aliasList.size() > 1 ) {
                networkString += " or one of its aliases";
            }
        }
        
        if ( !isInNetwork( natSettings.getDhcpStartAddress(), aliasList )) {
            throw new ValidateException( "\"IP Address Range Start\" in the DHCP panel must " + 
                                         "be in the network: " + networkString );
        }
            
        if ( !isInNetwork( natSettings.getDhcpEndAddress(), aliasList )) {
            throw new ValidateException( "\"IP Address Range End\" in the DHCP panel must "+
                                         "be in the network: " + networkString );
        }
            
        for ( DhcpLeaseRule rule : natSettings.getDhcpLeaseList()) {
            IPaddr address = rule.getStaticAddress();
            if ( address.getAddr() == null ) continue;
            
            if ( !isInNetwork( address, aliasList )) {
                throw new 
                    ValidateException( "\"target static IP address\" for DHCP Address Map entry '" +
                                       address.toString() + "' must be in the network: " + networkString );
            }
        }

        /* Validate that a MAC address or an IP address isn't repeated twice */
        Map macAddressSet = new HashMap();
        Map ipAddressSet = new HashMap();
        
        Integer c = 1;
        for ( DhcpLeaseRule rule : natSettings.getDhcpLeaseList()) {
            MACAddress macAddress = rule.getMacAddress();
            IPaddr ipAddress = rule.getStaticAddress();
            
            Integer index;
            if (( index = (Integer)macAddressSet.put( macAddress, c )) != null ) {
                throw new
                    ValidateException( "The \"MAC Address\" '" + macAddress + 
                                       "' in the DHCP Address Map " + 
                                       " is repeated at index " + index + " and " + c );
            }
            
            if (( ipAddress != null ) && !ipAddress.isEmpty() &&
                ( index = (Integer)ipAddressSet.put( ipAddress, c )) != null ) {
                throw new
                    ValidateException( "The \"target static ip address\" '" + ipAddress + 
                                       "' in the DHCP Address Map " + 
                                       " is repeated at index " + index + " and " + c );
            }
            c++;
        }
    }

    private boolean isInNetwork( IPaddr address, List<InterfaceAlias> aliasList )
    {
        for ( InterfaceAlias alias : aliasList ) {
            if ( address.isInNetwork( alias.getAddress(), alias.getNetmask())) return true;
        }
        
        return false;
    }

    static SettingsValidator getInstance()
    {
        return INSTANCE;
    }
}