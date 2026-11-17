/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

/**
 * Reviewed callable operations for SafePluginManager.
 */
public interface SafePluginManager
{
    /**
     * getPlugin through the reviewed RPC contract.
     * @param className The className argument
     * @return The delegated result
     */
    SafePlugin getPlugin(String className);

    /**
     * loadPlugins through the reviewed RPC contract.
     */
    void loadPlugins();
}
