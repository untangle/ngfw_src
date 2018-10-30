/**
 * $Id$
 */
package com.untangle.uvm;

/**
 * Abstraction to the daemon manager class
 */

public interface DaemonManager
{
    public void incrementUsageCount(String daemonName);

    public void decrementUsageCount(String daemonName);

    public void reload(String daemonName);

    public int getUsageCount(String daemonName);

    public String getStatus(String daemonName);

    public boolean enableDaemonMonitoring(String daemonName, long secondInterval, String processName);

    public boolean enableRequestMonitoring(String daemonName, long secondInterval, String hostString, int hostPort, String transmitString, String searchString);

    public void setExtraRestartCommand(String daemonName, String extraRestartCommand, long extraRestartDelay);

    public boolean disableAllMonitoring(String daemonName);

    public boolean isRunning(String daemonName);
}
