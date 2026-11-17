/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

/**
 * Reviewed callable operations for SafeNetcapManager.
 */
public interface SafeNetcapManager
{
    /**
     * setJNetcapDebugLevel through the reviewed RPC contract.
     * @param level The level argument
     */
    void setJNetcapDebugLevel(int level);

    /**
     * setNetcapDebugLevel through the reviewed RPC contract.
     * @param level The level argument
     */
    void setNetcapDebugLevel(int level);
}
