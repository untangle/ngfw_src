/**
 * $Id$
 */
package com.untangle.uvm.app;

import java.util.List;

/**
 * This interface provides the needed platform functionality of the Directory Connector.
 * This lives here so other apps can compile against these functions.
 * This interface is implemented by the Directory Connector app itself
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

    /**
     * Authenticate a user using Google
     */
    public boolean googleAuthenticate( String username, String group );

    /**
     * Authenticate a user using Facebook
     */
    public boolean facebookAuthenticate( String username, String group );
    
    /**
     * Authenticate a user using any authentication method
     */
    public boolean anyAuthenticate( String username, String group );
    
    /**
     * Return true if Google Drive is configured, false otherwise
     */
    public boolean isGoogleDriveConnected();
}
