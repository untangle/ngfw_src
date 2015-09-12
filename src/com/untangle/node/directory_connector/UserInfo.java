/**
 * $Id$
 */
package com.untangle.node.directory_connector;

import java.net.InetAddress;
import java.util.Date;

public class UserInfo
{
    /* Various states the user info object can be in */
    public enum LookupState
    { 
        UNITITIATED,     /* none of the assistants attempted to lookup information */
        IN_PROGRESS,     /* an assistant is attempting to lookup the information */
        FAILED,          /* an assistant attempted to lookup and failed */
        COMPLETED        /* lookup completed succesfully */
    };

    /* The address of the information */
    private final InetAddress address;

    /* If we want to get into network access control, we could also add MAC address here. */

    /* The time that the lookup was initiated */
    private final Date lookupTime;

    /* The expiration date (millis since epoch) */
    private long expirationDate;

    /* The username for this ip address */
    private String username;

    /* The hostname for this ip address */
    private String hostname;

    /* The method by which the username was capture/identified/authenticated */
    private String authMethod;
    
    /* Default state of the username lookup is pending */
    private LookupState usernameState = LookupState.UNITITIATED;
    
    private LookupState hostnameState = LookupState.UNITITIATED;

    public UserInfo( InetAddress address, long lifetimeMillis )
    {
        this( address, new Date(), null, null, lifetimeMillis );
    }

    /* generic helper */
    private UserInfo( InetAddress address, Date lookupTime, String username, String hostname, long lifetimeMillis )
    {
        this.address = address;
        this.lookupTime = lookupTime;
        this.username = username;
        this.hostname = hostname;
        this.expirationDate = System.currentTimeMillis() + lifetimeMillis;
    }

    public Date getLookupTime()
    {
        return this.lookupTime;
    }

    public boolean isExpired()
    {
        if (this.expirationDate == 0) /* never expires */
            return false;
        
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

    public String getUsername()
    {
        return this.username;
    }

    public void setUsername( String newValue )
    {
        this.username = newValue;
        /* update the state to completed */
        this.usernameState = LookupState.COMPLETED;
    }

    public String getAuthMethod()
    {
        return this.authMethod;
    }

    public void setAuthMethod( String newValue )
    {
        this.authMethod = newValue;
    }
    
    public LookupState getUsernameState()
    {
        return this.usernameState;
    }

    public void setUsernameState( LookupState newValue )
    {
        this.usernameState = newValue;
    }

    public String getHostname()
    {
        return this.hostname;
    }

    public void setHostname( String newValue )
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
        
        sb.append( "<ip: " +  this.address );
        sb.append( " time: " + this.lookupTime );
        sb.append( " user:[" + this.usernameState + "] " + this.username );
        sb.append( " host:[" + this.hostnameState + "] " + this.hostname + ">" );
        
        return sb.toString();
    }
}