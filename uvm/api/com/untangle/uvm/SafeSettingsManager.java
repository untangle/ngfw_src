/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

/**
 * Reviewed callable operations for SafeSettingsManager.
 */
public interface SafeSettingsManager
{
    /**
     * getDiff through the reviewed RPC contract.
     * @param fileName The fileName argument
     * @return The delegated result
     * @throws SettingsManager.SettingsException If the operation fails
     */
    String getDiff(String fileName) throws SettingsManager.SettingsException;
}
