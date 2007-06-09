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

package com.untangle.uvm.user;

import java.net.InetAddress;
import java.util.Date;

import com.untangle.uvm.node.HostName;
import com.untangle.uvm.user.Username;

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