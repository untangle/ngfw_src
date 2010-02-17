/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.user;

import java.net.InetAddress;
import java.util.Date;

import com.untangle.uvm.node.HostName;

public class UserInfo
{
    /* this is how long to assume a login is valid for (in millis) */
    static final long DEFAULT_LIFETIME_MILLIS = 3 * 60 * 1000;
    
    /* Various states the user info object can be in */
    public enum LookupState
    { 
        UNITITIATED,     /* none of the assistants attempted to lookup information */
        IN_PROGRESS,     /* an assistant is attempting to lookup the information */
        FAILED,          /* an assistant attempted to lookup and failed */
        COMPLETED        /* lookup completed succesfully */
    };

    /* This is the key used to lookup the data, this is an ID that can
     * be referenced in the future to find out user information */
    private final long lookupKey;

    /* The address of the information */
    private final InetAddress address;

    /* If we want to get into network access control, we could also add MAC address here. */

    /* The time that the lookup was initiated */
    private final Date lookupTime;

    /* The expiration date (millis since epoch) */
    private long expirationDate;

    /* The username for this ip address */
    private Username username;

    /* The hostname for this ip address */
    private HostName hostname;
    
    /* Default state of the username lookup is pending */
    private LookupState usernameState = LookupState.UNITITIATED;
    
    private LookupState hostnameState = LookupState.UNITITIATED;

    private UserInfo( long lookupKey, InetAddress address, Date lookupTime, long lifetimeMillis )
    {
        this( lookupKey, address, lookupTime, null, null, lifetimeMillis );
    }
    /* generic helper */
    private UserInfo( long lookupKey, InetAddress address, Date lookupTime, Username username, 
                      HostName hostname, long lifetimeMillis )
    {
        this.lookupKey = lookupKey;
        this.address = address;
        this.lookupTime = lookupTime;
        this.username = username;
        this.hostname = hostname;
        this.expirationDate = System.currentTimeMillis() + lifetimeMillis;
    }

    public long getLookupKey()
    {
        return this.lookupKey;
    }

    public Date getLookupTime()
    {
        return this.lookupTime;
    }

    public boolean isExpired()
    {
        return ( System.currentTimeMillis() > expirationDate );
    }

    public void setExpirationDate( long newValue )
    {
        this.expirationDate = newValue;
    }

    public InetAddress getAddress()
    {
        return this.address;
    }

    public Username getUsername()
    {
        return this.username;
    }

    public void setUsername( Username newValue )
    {
        this.username = newValue;
        /* update the state to completed */
        this.usernameState = LookupState.COMPLETED;
    }

    public LookupState getUsernameState()
    {
        return this.usernameState;
    }

    public void setUsernameState( LookupState newValue )
    {
        this.usernameState = newValue;
    }

    public HostName getHostname()
    {
        return this.hostname;
    }

    public void setHostname( HostName newValue )
    {
        this.hostname = newValue;
        /* update the state to completed */
        this.hostnameState = LookupState.COMPLETED;
    }

    public LookupState getHostnameState()
    {
        return this.hostnameState;
    }

    public void setHostnameState( LookupState newValue )
    {
        this.hostnameState = newValue;
    }
    
    /* has data if the hostname or the username is non-null */
    public boolean hasData()
    {
        return ( this.hostnameState == LookupState.COMPLETED && this.hostname != null ) ||
            ( this.usernameState == LookupState.COMPLETED && this.username != null );
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "<key: " + this.lookupKey );
        sb.append( " ip: " +  this.address );
        sb.append( " time: " + this.lookupTime );
        sb.append( " user:[" + this.usernameState + "] " + this.username );
        sb.append( " host:[" + this.hostnameState + "] " + this.hostname + ">" );
        
        return sb.toString();
    }

    /* Create an initial user lookup instance */
    static UserInfo makeInstance( long lookupKey, InetAddress address )
    {
        return makeInstance( lookupKey, address, DEFAULT_LIFETIME_MILLIS );
    }

    static UserInfo makeInstance( long lookupKey, InetAddress address, long lifetimeMillis )
    {
        if ( lifetimeMillis <= 10 ) lifetimeMillis = DEFAULT_LIFETIME_MILLIS;
        return new UserInfo( lookupKey, address, new Date(), lifetimeMillis );
    }
}