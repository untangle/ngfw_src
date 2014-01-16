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
        DaemonObject(String daemonName)
        {
            this.daemonName = daemonName;
        }

        String daemonName = null;
        int usageCount = 0;
        String monitorName = null;
        long monitorInterval = 0;
        long lastCheck = 0;
    }

    /**
     * Stores a map from the daemon name to daemon object which holds the
     * current usage count along with monitoring details.
     */
    private ConcurrentHashMap<String, DaemonObject> daemonHashMap = new ConcurrentHashMap<String, DaemonObject>();

    public DaemonManagerImpl()
    {
        timer.scheduleAtFixedRate(this, 15000, 15000);
    }

    public void run()
    {
        long currentTime = System.currentTimeMillis();

        for (DaemonObject object : daemonHashMap.values()) {
            // ignore objects not configured for monitoring
            if (object.monitorInterval == 0)
                continue;

            // ignore objects that don't need to be checked yet
            if (currentTime < (object.lastCheck + object.monitorInterval))
                continue;

            // check the process and update the timestamp 
            checkDaemonProcess(object);
            object.lastCheck = currentTime;
        }
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

        if (newUsageCount < 0) {
            logger.warn("Invalid daemon usageCount for " + daemonName + ": " + newUsageCount);
        }

        setUsageCount(daemonName, newUsageCount);

        if (newUsageCount < 1) {
            // first we should disable any monitoring that was enabled
            disableDaemonMonitoring(daemonName);

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

    public synchronized boolean enableDaemonMonitoring(String daemonName, String monitorName, long secondInterval)
    {
        DaemonObject daemonObject = daemonHashMap.get(daemonName);
        if (daemonObject == null)
            return (false);

        daemonObject.monitorName = monitorName;
        daemonObject.monitorInterval = (secondInterval * 1000);
        daemonObject.lastCheck = System.currentTimeMillis();
        return (true);
    }

    public synchronized boolean disableDaemonMonitoring(String daemonName)
    {
        DaemonObject daemonObject = daemonHashMap.get(daemonName);
        if (daemonObject == null)
            return (false);

        daemonObject.monitorName = null;
        daemonObject.monitorInterval = 0;
        daemonObject.lastCheck = 0;
        return (true);
    }

    // these function are private since they should not be access externally

    private synchronized int getUsageCount(String daemonName)
    {
        DaemonObject daemonObject = daemonHashMap.get(daemonName);
        if (daemonObject == null) {
            daemonObject = new DaemonObject(daemonName);
            daemonHashMap.put(daemonName, daemonObject);
        }

        return daemonObject.usageCount;
    }

    private synchronized void setUsageCount(String daemonName, int usageCount)
    {
        logger.info("Daemon " + daemonName + " usageCount: " + usageCount);
        DaemonObject daemonObject = daemonHashMap.get(daemonName);

        if (daemonObject == null) {
            daemonObject = new DaemonObject(daemonName);
            daemonHashMap.put(daemonName, daemonObject);
        }

        daemonObject.usageCount = usageCount;
    }

    private synchronized void checkDaemonProcess(DaemonObject object)
    {
        String result = UvmContextFactory.context().execManager().execOutput("ps -e | grep " + object.monitorName + " | wc -l");
        int count = Integer.parseInt(result.replaceAll("[^0-9]", ""));

        // if we find the process running just return
        if (count > 0)
            return;

        // process does not seem to be running so log and restart
        logger.warn("Restarting failed daemon: " + object.daemonName);
        String output = UvmContextFactory.context().execManager().execOutput("/etc/init.d/" + object.daemonName + " restart");
    }
}
