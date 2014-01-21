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

    public boolean enableDaemonMonitoring(String daemonName, long secondInterval, String processName);

    public boolean enableRequestMonitoring(String daemonName, long secondInterval, String hostString, int hostPort, String transmitString, String searchString);

    public boolean disableAllMonitoring(String daemonName);
}
