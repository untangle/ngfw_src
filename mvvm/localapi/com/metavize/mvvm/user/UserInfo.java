/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.user;

import java.net.InetAddress;
import java.util.Date;

import com.metavize.mvvm.tran.HostName;
import com.metavize.mvvm.user.Username;

public final class UserInfo
{
    /* Various states the user info object can be in */
    public enum LookupState
    { 
        IN_PROGRESS, FAILED, COMPLETED
    };

    /* This is the key used to lookup the data, this is an ID that can
     * be referenced in the future to find out user information */
    private final long userLookupKey;

    /* The address of the information */
    private final InetAddress address;

    /* If we want to get into network access control, we could also add MAC address here. */

    /* The time that the lookup was initiated */
    private final Date lookupTime;

    /* The username for this ip address */
    private Username username;

    /* The hostname for this ip address */
    private HostName hostname;
    
    /* Current state is pending */
    private LookupState state = LookupState.IN_PROGRESS;

    private UserInfo( long userLookupKey, InetAddress address, Date lookupTime )
    {
        this( userLookupKey, address, lookupTime, null, null );
    }
    /* generic helper */
    private UserInfo( long userLookupKey, InetAddress address, Date lookupTime, Username username, 
                      HostName hostname )
    {
        this.userLookupKey = userLookupKey;
        this.address = address;
        this.lookupTime = lookupTime;
        this.username = username;
        this.hostname = hostname;
    }
    

    public long getUserLookupKey()
    {
        return this.userLookupKey;
    }

    public Date getLookupTime()
    {
        return this.lookupTime;
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
    }

    public HostName getHostname()
    {
        return this.hostname;
    }

    public void setHostname( HostName newValue )
    {
        this.hostname = newValue;
    }

   public LookupState getState()
    {
        return this.state;
    }

    public void setState( LookupState newValue )
    {
        this.state = newValue;
    }

    /* Create an initial user lookup instance */
    static UserInfo makeInstance( long userLookupKey, InetAddress address )
    {
        return new UserInfo( userLookupKey, address, new Date());
    }
}