/**
 * $Id$
 */
package com.untangle.node.directory_connector;

import java.util.List;
import java.util.Map;

import javax.naming.ServiceUnavailableException;

/**
 * This is the ActiveDirectoryManager API (used by the GUI)
 */
public interface ActiveDirectoryManager
{
    public String getActiveDirectoryStatusForSettings( DirectoryConnectorSettings newSettings );

    public List<UserEntry> getActiveDirectoryUserEntries() throws ServiceUnavailableException;

    public List<GroupEntry> getActiveDirectoryGroupEntries( boolean fetchMemberOf );

    public Map<String,String> getUserGroupMap();
}
