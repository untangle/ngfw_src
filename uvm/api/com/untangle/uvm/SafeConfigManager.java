/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

/**
 * Reviewed callable operations for SafeConfigManager.
 */
public interface SafeConfigManager
{
    /**
     * doFactoryReset through the reviewed RPC contract.
     * @return The delegated result
     */
    Object doFactoryReset();
}
