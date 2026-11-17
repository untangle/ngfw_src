/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

/**
 * Reviewed callable operations for SafeDeviceTable.
 */
public interface SafeDeviceTable
{
    /**
     * getDevicesSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    DevicesSettings getDevicesSettings();

    /**
     * setDevicesSettings through the reviewed RPC contract.
     * @param newSettings The newSettings argument
     */
    void setDevicesSettings(DevicesSettings newSettings);
}
