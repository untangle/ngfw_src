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
package com.untangle.tran.openvpn;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.untangle.mvvm.tran.HostName;
import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.user.Assistant;
import com.untangle.mvvm.user.UserInfo;
import com.untangle.mvvm.user.Username;
import org.apache.log4j.Logger;

class PhoneBookAssistant implements Assistant
{
    private final int PRIORITY = 1;

    /* These are the special addresses that are inside of the DNS map */
    private Map<InetAddress,Data> userMap = Collections.emptyMap();

    /* determines whether or not VPN is presently enabled */
    private boolean isVpnEnabled = false;

    private final Logger logger = Logger.getLogger( getClass());

    /* -------------- Constructors -------------- */
    PhoneBookAssistant()
    {
    }

    /* ----------------- Public ----------------- */
    public void lookup( UserInfo info )
    {
        InetAddress address = info.getAddress();

        if ( !this.isVpnEnabled ) return;

        /* Check the user map */
        Map<InetAddress,Data> currentMap = this.userMap;

        Data d = currentMap.get( address );

        if ( d != null ) d.fillInfo( info );
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
    void configure( VpnSettings settings, boolean isEnabled )
    {
        Map<InetAddress,Data> newMap = null;

        if ( !isEnabled || settings.isUntanglePlatformClient()) {
            /* If dns is disabled, then the dns addresses don't really matter */
            newMap = Collections.emptyMap();
            this.isVpnEnabled = false;
        } else {
            newMap = new HashMap<InetAddress,Data>();
            for ( VpnClientBase client : settings.getClientList()) {
                /* ignore the disabled rules */
                if ( !client.isEnabled()) continue;

                /* Attempt to convert the client name into a hostname */
                Username u = null;
                HostName h = null;
                String name = client.getName();

                try {
                    u = Username.parse( name );
                } catch ( ParseException e ) {
                    logger.info( "unable to parse the client name '" + name + "' as a username" );
                }

                try {
                    h = HostName.parse( name );
                } catch ( ParseException e ) {
                    logger.info( "unable to parse the client name '" + name + "' as a hostname" );
                }

                newMap.put( client.getAddress().getAddr(), new Data( u, h ));
            }

            this.isVpnEnabled = true;
        }

        /* Save the new map */
        this.userMap = Collections.unmodifiableMap( newMap );
    }

    private static class Data
    {

        private final Username username;
        private final HostName hostname;

        Data( Username u, HostName h )
        {
            this.username = u;
            this.hostname = h;
        }

        void fillInfo( UserInfo info )
        {
            if ( this.username != null ) info.setUsername( this.username );
            if ( this.hostname != null ) info.setHostname( this.hostname );
        }
    }
}
