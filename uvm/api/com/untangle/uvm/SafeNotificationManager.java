/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.List;

/**
 * Reviewed callable operations for SafeNotificationManager.
 */
public interface SafeNotificationManager
{
    /**
     * getNotifications through the reviewed RPC contract.
     * @return The delegated result
     */
    List<String> getNotifications();
}
