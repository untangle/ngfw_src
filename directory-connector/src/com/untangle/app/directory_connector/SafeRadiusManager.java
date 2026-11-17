/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.app.directory_connector;

/**
 * Reviewed callable operations for SafeRadiusManager.
 */
public interface SafeRadiusManager
{
    /**
     * getRadiusStatusForSettings through the reviewed RPC contract.
     * @param newSettings The newSettings argument
     * @param username The username argument
     * @param password The password argument
     * @return The delegated result
     */
    String getRadiusStatusForSettings(DirectoryConnectorSettings newSettings, String username, String password);
}
