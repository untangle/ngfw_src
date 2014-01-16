/**
 * $Id: DaemonManager.java,v 1.00 2013/10/15 12:44:50 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.util.TimerTask;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.DaemonManager;

/**
 * This is a utility class for starting/stopping daemons and keeping reference
 * counts so they are automatically started when 1 or more apps require them and
 * automatically stopped when no one requires them. It also provides very basic
 * daemon monitoring and restart logic.
 */
public class DaemonManagerImpl extends TimerTask implements DaemonManager
{
    private final Logger logger = Logger.getLogger(getClass());
    private final Timer timer = new Timer("DaemonManager", true);

    class DaemonObject
    {
        int usageCount = 0;
        long monitorInterval = 0;
        long lastCheck = 0;
        String processName = null;
    }

    /**
     * Stores a map from the daemon name to daemon object which holds the
     * current usage count along with monitoring details.
     */
    private ConcurrentHashMap<String, DaemonObject> usageCountMap = new ConcurrentHashMap<String, DaemonObject>();

    public DaemonManagerImpl()
    {
        timer.schedule(this, 1000, 1000);
    }

    public void run()
    {
    }

    public synchronized void incrementUsageCount(String daemonName)
    {
        int newUsageCount = getUsageCount(daemonName) + 1;
        setUsageCount(daemonName, newUsageCount);

        if (newUsageCount == 1) {
            String cmd = "/etc/init.d/" + daemonName + " start";
            String output = UvmContextFactory.context().execManager().execOutput(cmd);
            try {
                String lines[] = output.split("\\r?\\n");
                for (String line : lines)
                    logger.info(cmd + ": " + line);
            } catch (Exception e) {
            }
        }
    }

    public synchronized void decrementUsageCount(String daemonName)
    {
        int newUsageCount = getUsageCount(daemonName) - 1;
        setUsageCount(daemonName, newUsageCount);

        if (newUsageCount < 0) {
            logger.warn("Invalid daemon usageCount for " + daemonName + ": " + newUsageCount);
        }

        if (newUsageCount < 1) {
            String cmd = "/etc/init.d/" + daemonName + " stop";
            String output = UvmContextFactory.context().execManager().execOutput(cmd);
            try {
                String lines[] = output.split("\\r?\\n");
                for (String line : lines)
                    logger.info(cmd + ": " + line);
            } catch (Exception e) {
            }
        }
    }

    public synchronized boolean enableDamonMonitoring(String daemonName, String processName, long secondInterval)
    {
        DaemonObject daemonObject = usageCountMap.get(daemonName);
        if (daemonObject == null)
            return (false);

        daemonObject.processName = processName;
        daemonObject.monitorInterval = (secondInterval * 1000);
        daemonObject.lastCheck = System.currentTimeMillis();
        return (true);
    }

    public synchronized boolean disableDaemonMonitoring(String daemonName)
    {
        DaemonObject daemonObject = usageCountMap.get(daemonName);
        if (daemonObject == null)
            return (false);

        daemonObject.processName = null;
        daemonObject.monitorInterval = 0;
        daemonObject.lastCheck = 0;
        return (true);
    }

    // these function are private since they should not be access externally

    private synchronized int getUsageCount(String daemonName)
    {
        DaemonObject daemonObject = usageCountMap.get(daemonName);
        if (daemonObject == null) {
            daemonObject = new DaemonObject();
            usageCountMap.put(daemonName, daemonObject);
        }

        return daemonObject.usageCount;
    }

    private synchronized void setUsageCount(String daemonName, int usageCount)
    {
        logger.info("Daemon " + daemonName + " usageCount: " + usageCount);
        DaemonObject daemonObject = usageCountMap.get(daemonName);

        if (daemonObject == null) {
            daemonObject = new DaemonObject();
            usageCountMap.put(daemonName, daemonObject);
        }

        daemonObject.usageCount = usageCount;
    }
}
