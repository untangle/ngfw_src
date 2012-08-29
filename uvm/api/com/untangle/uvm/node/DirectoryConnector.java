/**
 * $Id: DirectoryConnector.java,v 1.00 2011/08/15 14:13:23 dmorris Exp $
 */
package com.untangle.uvm.node;

import java.util.List;

/**
 * This interface provides the needed platform functionality of the Directory Connector.
 * This lives here so other nodes can compile against these functions.
 * This interface is implemented by the Directory Connector node itself
 */
public interface DirectoryConnector 
{
    /**
     * Query if a user is a member of a group (Currently this only applies to Active Directory)
     */
    public boolean isMemberOf( String username, String group );

    /**
     * Retrieve a list of groups this user belongs to (Currently only applies to Active Directory)
     */
    public List<String> memberOf(String user);
    
    /**
     * Authenticate a user against Active Directory
     */
    public boolean activeDirectoryAuthenticate( String username, String group );

    /**
     * Authenticate a user using RADIUS
     */
    public boolean radiusAuthenticate( String username, String group );
}
