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

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.untangle.mvvm.tran.HostNameList;
import com.untangle.mvvm.tran.HostName;

import com.untangle.mvvm.networking.internal.DnsStaticHostInternal;
import com.untangle.mvvm.networking.internal.ServicesInternalSettings;

import com.untangle.mvvm.user.Assistant;
import com.untangle.mvvm.user.UserInfo;


class PhoneBookAssistant implements Assistant
{
    private final int PRIORITY = 0;

    private final Logger logger = Logger.getLogger( getClass());

    /* These are the special addresses that are inside of the DNS map */
    private Map<InetAddress,HostName> dnsAddressMap = Collections.emptyMap();

    private boolean isEnabled = false;

    private boolean isDhcpEnabled = false;

    private final DhcpMonitor dhcpMonitor;
    
    /* -------------- Constructors -------------- */
    PhoneBookAssistant( DhcpMonitor dhcpMonitor )
    {
        this.dhcpMonitor = dhcpMonitor;
    }
    
    /* ----------------- Public ----------------- */
    public void lookup( UserInfo info )
    {
        InetAddress address = info.getAddress();

        /* nothing to do if the services are disabled */
        if ( !isEnabled ) return;

        /* First check the dns address map */
        Map<InetAddress,HostName> currentMap = this.dnsAddressMap;

        HostName h = currentMap.get( address );
        
        /* No hit, have to check the current DHCP map */
        if ( h == null && this.isDhcpEnabled ) {
            DhcpLease lease = dhcpMonitor.lookupLease( address );
            
            /* only report active leases */
            if ( lease != null && lease.isActive()) h = lease.getHostname();
        }

        if ( logger.isDebugEnabled()) logger.debug( "found the hostname: " + h );

        if ( h != null ) info.setHostname( h );
    }

    /* Check to see if the user information has changed, if it has return a new UserInfo object */
    public UserInfo update( UserInfo info )
    {
        throw new IllegalStateException( "unimplemented" );
    }

    public int priority()
    {
        return PRIORITY;
    }

    /* ---------------- Package ----------------- */
    /** Create a new map  from the current settings. */
    void configure( ServicesInternalSettings servicesSettings )
    {
        Map<InetAddress,HostName> newMap = null;

        if ( servicesSettings.getIsDnsEnabled()) {
            /* If dns is disabled, then the dns addresses don't really matter */
            newMap = Collections.emptyMap();
        } else {
            newMap = new HashMap<InetAddress,HostName>();
            for ( DnsStaticHostInternal internal : servicesSettings.getDnsStaticHostList()) {
                /* ignore the disabled rules */
                if ( !internal.isLive()) continue;

                /* ignore the second instance of a rule */
                InetAddress address = internal.getStaticAddress().getAddr();
                if ( newMap.containsKey( address )) continue;

                HostNameList hnl = internal.getHostNameList();

                if ( hnl.isEmpty()) continue;
                
                /* Add the first hostname in the list */
                newMap.put( address, hnl.getHostNameList().get( 0 ));
            }
        }

        /* Save the new map */
        this.dnsAddressMap = Collections.unmodifiableMap( newMap );

        /* Update whether or not DHCP is enabled */
        this.isDhcpEnabled = servicesSettings.getIsDhcpEnabled();

        /* update whether or not the services are enabled */
        this.isEnabled = servicesSettings.getIsEnabled();
    }
}
