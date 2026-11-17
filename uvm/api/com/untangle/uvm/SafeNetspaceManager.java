/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

/**
 * Reviewed callable operations for SafeNetspaceManager.
 */
public interface SafeNetspaceManager
{
    /**
     * isNetworkAvailable through the reviewed RPC contract.
     * @param ownerName The ownerName argument
     * @param networkText The networkText argument
     * @return The delegated result
     */
    NetspaceManager.NetworkSpace isNetworkAvailable(String ownerName, String networkText);
}
