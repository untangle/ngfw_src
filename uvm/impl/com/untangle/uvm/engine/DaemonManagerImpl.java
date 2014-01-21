/**
 * $Id: DaemonManager.java,v 1.00 2013/10/15 12:44:50 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.util.TimerTask;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.log4j.Logger;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.DaemonManager;

/**
 * This is a utility class for starting/stopping daemons and keeping reference
 * counts so they are automatically started when 1 or more apps require them and
 * automatically stopped when no one requires them. It also provides very basic
 * process or request monitoring and restart logic.
 */

public class DaemonManagerImpl extends TimerTask implements DaemonManager
{
    private final Logger logger = Logger.getLogger(getClass());
    private final Timer timer = new Timer("DaemonManager", true);

    enum MonitorType
    {
        DISABLED, DAEMON, REQUEST
    }

    class DaemonObject
    {
        DaemonObject(String daemonName)
        {
            this.daemonName = daemonName;
        }

        String daemonName = null;
        int usageCount = 0;
        MonitorType monitorType = MonitorType.DISABLED;
        String transmitString = null;
        String searchString = null;
        long monitorInterval = 0;
        long lastCheck = 0;
        String hostString = null;
        int hostPort = 0;
    }

    /**
     * Stores a map from the daemon name to daemon object which holds the
     * current usage count along with monitoring details.
     */
    private ConcurrentHashMap<String, DaemonObject> daemonHashMap = new ConcurrentHashMap<String, DaemonObject>();

    public DaemonManagerImpl()
    {
        // we'll do the monitoring stuff every 5 seconds 
        timer.scheduleAtFixedRate(this, 5000, 5000);
    }

    public void run()
    {
        for (DaemonObject object : daemonHashMap.values()) {
            // ignore objects not configured for monitoring
            if (object.monitorType == MonitorType.DAEMON)
                continue;

            // ignore objects that don't need to be checked yet
            if (System.currentTimeMillis() < (object.lastCheck + object.monitorInterval))
                continue;

            // do the actual check based on monitor configuration
            switch (object.monitorType)
            {
            case DAEMON:
                handleDaemonCheck(object);
                break;
            case REQUEST:
                handleRequestCheck(object);
                break;
            }

            // update the last check timestamp
            object.lastCheck = System.currentTimeMillis();
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
            disableAllMonitoring(daemonName);

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

    public synchronized boolean enableDaemonMonitoring(String daemonName, long secondInterval, String searchString)
    {
        DaemonObject daemonObject = daemonHashMap.get(daemonName);
        if (daemonObject == null)
            return (false);

        daemonObject.monitorType = MonitorType.DAEMON;
        daemonObject.monitorInterval = (secondInterval * 1000);
        daemonObject.searchString = searchString;
        daemonObject.lastCheck = System.currentTimeMillis();
        return (true);
    }

    public synchronized boolean enableRequestMonitoring(String daemonName, long secondInterval, String hostString, int hostPort, String transmitString, String searchString)
    {
        DaemonObject daemonObject = daemonHashMap.get(daemonName);
        if (daemonObject == null)
            return (false);

        daemonObject.monitorType = MonitorType.REQUEST;
        daemonObject.monitorInterval = (secondInterval * 1000);
        daemonObject.hostString = hostString;
        daemonObject.hostPort = hostPort;
        daemonObject.transmitString = transmitString;
        daemonObject.searchString = searchString;
        daemonObject.lastCheck = System.currentTimeMillis();
        return (true);
    }

    public synchronized boolean disableAllMonitoring(String daemonName)
    {
        DaemonObject daemonObject = daemonHashMap.get(daemonName);
        if (daemonObject == null)
            return (false);

        daemonObject.transmitString = null;
        daemonObject.searchString = null;
        daemonObject.monitorInterval = 0;
        daemonObject.lastCheck = 0;
        daemonObject.hostString = null;
        daemonObject.hostPort = 0;
        daemonObject.monitorType = MonitorType.DISABLED;
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

    private synchronized void handleDaemonCheck(DaemonObject object)
    {
        // run a spiffy command to count the number of process instances
        String result = UvmContextFactory.context().execManager().execOutput("ps -e | grep " + object.searchString + " | wc -l");
        int count = Integer.parseInt(result.replaceAll("[^0-9]", ""));

        // if we find the process running just return
        if (count > 0)
            return;

        // process does not seem to be running so log and restart

        logger.warn("Restarting failed daemon: " + object.daemonName);

        String cmd = "/etc/init.d/" + object.daemonName + " restart";
        String output = UvmContextFactory.context().execManager().execOutput(cmd);
        try {
            String lines[] = output.split("\\r?\\n");
            for (String line : lines)
                logger.info(cmd + ": " + line);
        } catch (Exception e) {
        }
    }

    private synchronized void handleRequestCheck(DaemonObject object)
    {
        ByteBuffer txbuffer = ByteBuffer.allocate(object.transmitString.length());
        ByteBuffer rxbuffer = ByteBuffer.allocate(4096);
        SocketChannel netSocket = null;
        boolean restart = false;
        int txcount, rxcount;

        // put the transmit string in the buffer and prepare for sending
        txbuffer.put(object.transmitString.getBytes());
        txbuffer.flip();

        // connect, send txbuffer an receive rxbuffer
        try {
            netSocket = SocketChannel.open();
            InetSocketAddress socketAddress = new InetSocketAddress(object.hostString, object.hostPort);
            netSocket.connect(socketAddress);
            txcount = netSocket.write(txbuffer);
            rxcount = netSocket.read(rxbuffer);
        }

        // catch and log any exceptions and set the restart flag
        catch (Exception exn) {
            String reason = exn.getMessage();
            if (reason == null)
                reason = exn.getCause().toString();
            if (reason == null)
                reason = exn.getClass().toString();
            if (reason == null)
                reason = "unknown";

            logger.warn("Exception (" + reason + ") while checking " + object.hostString + ":" + object.hostPort);
            restart = true;
        }

        // make sure the socket gets closed ignoring any exceptions
        try {
            if (netSocket != null)
                netSocket.close();
        } catch (Exception exn) {
        }

        // if no exceptions then we check the response for the search string
        if (restart == false) {
            String haystack = new String(rxbuffer.array(), 0, rxbuffer.position());
            if (haystack.contains(object.searchString) == false)
                restart = true;
        }

        // if restart is we can just return
        if (restart == false)
            return;

        logger.warn("Restarting failed daemon: " + object.daemonName);

        String cmd = "/etc/init.d/" + object.daemonName + " restart";
        String output = UvmContextFactory.context().execManager().execOutput(cmd);
        try {
            String lines[] = output.split("\\r?\\n");
            for (String line : lines)
                logger.info(cmd + ": " + line);
        } catch (Exception e) {
        }
    }
}
