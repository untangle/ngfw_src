/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.app.directory_connector;

import javax.naming.ServiceUnavailableException;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Reviewed callable operations for SafeActiveDirectoryManager.
 */
public interface SafeActiveDirectoryManager
{
    /**
     * getStatusForSettings through the reviewed RPC contract.
     * @param testServerSettings The testServerSettings argument
     * @return The delegated result
     */
    JSONObject getStatusForSettings(ActiveDirectoryServer testServerSettings);

    /**
     * getUsers through the reviewed RPC contract.
     * @param domain The domain argument
     * @return The delegated result
     * @throws ServiceUnavailableException If the operation fails
     */
    JSONArray getUsers(String domain) throws ServiceUnavailableException;
}
