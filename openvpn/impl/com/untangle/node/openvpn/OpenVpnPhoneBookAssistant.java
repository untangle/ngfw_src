/*
 * $HeadURL$
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
package com.untangle.node.openvpn;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.user.PhoneBookAssistant;
import com.untangle.uvm.user.UserInfo;

class OpenVpnPhoneBookAssistant implements PhoneBookAssistant
{
    private final int PRIORITY = 1;

    /* These are the special addresses that are inside of the DNS map */
    private Map<InetAddress,Data> userMap = Collections.emptyMap();

    /* determines whether or not VPN is presently enabled */
    private boolean isVpnEnabled = false;

    private final Logger logger = Logger.getLogger( getClass());

    /* -------------- Constructors -------------- */
    OpenVpnPhoneBookAssistant()
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
                String u = null;
                HostName h = null;
                String name = client.getName();
                u = name.trim();

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

        private final String username;
        private final HostName hostname;

        Data( String u, HostName h )
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
