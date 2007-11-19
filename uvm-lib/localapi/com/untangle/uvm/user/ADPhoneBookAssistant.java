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
package com.untangle.uvm.user;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.user.Assistant;
import com.untangle.uvm.user.UserInfo;
import com.untangle.uvm.user.Username;
import org.apache.log4j.Logger;

public class ADPhoneBookAssistant implements Assistant
{
    private final int PRIORITY = 1;

    /* These are the special addresses that are inside of the DNS map */
    private Map<InetAddress,Data> userMap =  new ConcurrentHashMap<InetAddress,Data>();

    private final Logger logger = Logger.getLogger( getClass());

    /* -------------- Constructors -------------- */
    ADPhoneBookAssistant()
    {
    }

    /* ----------------- Public ----------------- */
    public void lookup( UserInfo info )
    {
        InetAddress address = info.getAddress();

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
    public void addOrUpdate(InetAddress inetAddress, String username, String domain, String hostname)
    {
        try {
            Username u = Username.parse( username );
            HostName h = HostName.parse( hostname );
            userMap.put( inetAddress, new Data( u, h ) );
        } catch ( ParseException e ) {
            logger.info( "unable to parse username '" + username + "' or hostname '" + hostname  + "'" );
        }
    }

    public String toString() {
	StringBuffer results = new StringBuffer();
        Iterator<InetAddress> keyIterator = userMap.keySet().iterator();
	InetAddress key;
	Data data;
	while ( keyIterator.hasNext() ) {
            key = keyIterator.next();
            data = userMap.get( key );
	    results.append( key.toString() + ": " + data.toString() + "\n" );
	}
        return results.toString();
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
	public String toString() {
		return username.toString()+" "+hostname.toString();
	}
    }
}
