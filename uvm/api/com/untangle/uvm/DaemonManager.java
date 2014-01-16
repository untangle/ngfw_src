/*
 * $Id: DaemonManager.java 36469 2013-11-21 20:11:48Z dmorris $
 */

package com.untangle.uvm;

/**
 * Abstraction to the daemon manager class
 */

public interface DaemonManager
{
    public void incrementUsageCount(String daemonName);

    public void decrementUsageCount(String daemonName);

    public boolean enableDamonMonitoring(String daemonName, String processName, long secondInterval);

    public boolean disableDaemonMonitoring(String daemonName);
}
