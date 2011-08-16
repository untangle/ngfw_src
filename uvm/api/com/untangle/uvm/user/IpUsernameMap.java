/*
 * $Id: IpUsernameMap.java,v 1.00 2011/08/16 13:30:51 dmorris Exp $
 */
package com.untangle.uvm.user;

import java.net.InetAddress;

/**
 * This map stores the current IP - username map
 */
public interface IpUsernameMap
{
    /**
     * Lookup the username for a given IP
     */
    public String lookupUser( InetAddress address );

    /**
     * Lookup the username for a given IP
     */
    public String tryLookupUser( InetAddress address );

    /**
     * Expires the username for a given IP
     */
    public void expireUser ( InetAddress address );

    /**
     * Refresh the username for a given IP
     * This will call all the assistants again
     */
    public void refreshUser ( InetAddress address );
    
    /**
     * Register an assistant for the username map
     */
    public void registerAssistant( IpUsernameMapAssistant newAssistant );

    /**
     * Unregister an assistant for the username map
     */
    public void unregisterAssistant( IpUsernameMapAssistant assistant );

}
