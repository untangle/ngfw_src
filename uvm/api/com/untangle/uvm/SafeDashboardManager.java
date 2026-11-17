/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

/**
 * Reviewed callable operations for SafeDashboardManager.
 */
public interface SafeDashboardManager
{
    /**
     * getSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    DashboardSettings getSettings();

    /**
     * resetSettingsToDefault through the reviewed RPC contract.
     */
    void resetSettingsToDefault();

    /**
     * setSettings through the reviewed RPC contract.
     * @param settings The settings argument
     */
    void setSettings(DashboardSettings settings);
}
