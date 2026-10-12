/**
 * $Id$
 */
package com.untangle.uvm;

/**
 * Daemon status operations available to the administration interface.
 */
public interface SafeDaemonManager
{
    /**
     * Check whether a daemon is running.
     *
     * @param daemonName The daemon name
     * @return True if the daemon is running, otherwise false
     */
    boolean isRunning(String daemonName);
}
