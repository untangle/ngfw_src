/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.TimerTask;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.DataInputStream;

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
        String extraRestartCommand = null;
        long extraRestartDelay = 0;
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
            if (object.monitorType == MonitorType.DISABLED)
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

    public int getUsageCount(String daemonName)
    {
        DaemonObject daemonObject = getDaemonObject( daemonName );
        return daemonObject.usageCount;
    }

    public void incrementUsageCount(String daemonName)
    {
        DaemonObject daemonObject = getDaemonObject( daemonName );
        synchronized( daemonObject ) {
            int newUsageCount = daemonObject.usageCount + 1;
            daemonObject.usageCount = newUsageCount;

            if (newUsageCount == 1) {
                execDaemonControlEvil(daemonName, "start");
            }
        }
    }

    public void decrementUsageCount(String daemonName)
    {
        DaemonObject daemonObject = getDaemonObject( daemonName );
        synchronized( daemonObject ) {
            int newUsageCount = daemonObject.usageCount - 1;
            daemonObject.usageCount = newUsageCount;

            if (newUsageCount < 0) {
                logger.warn("Invalid daemon usageCount for " + daemonName + ": " + newUsageCount);
            }

            if (newUsageCount < 1) {
                // first we should disable any monitoring that was enabled
                disableAllMonitoring(daemonName);
                execDaemonControlEvil(daemonName, "stop");
            }
        }
    }
    
    public void setExtraRestartCommand(String daemonName, String extraRestartCommand, long extraRestartDelay)
    {
        DaemonObject daemonObject = getDaemonObject( daemonName );
        synchronized( daemonObject ) {
            daemonObject.extraRestartCommand = extraRestartCommand;
            daemonObject.extraRestartDelay = extraRestartDelay;
        }        
    }

    public boolean enableDaemonMonitoring(String daemonName, long secondInterval, String searchString)
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

    public boolean enableRequestMonitoring(String daemonName, long secondInterval, String hostString, int hostPort, String transmitString, String searchString)
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

    public boolean disableAllMonitoring(String daemonName)
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

    private synchronized DaemonObject getDaemonObject( String daemonName )
    {
        DaemonObject daemonObject = daemonHashMap.get(daemonName);

        if (daemonObject == null) {
            daemonObject = new DaemonObject(daemonName);
            daemonHashMap.put(daemonName, daemonObject);
        }

        return daemonObject;
    }

    /**
     * executes daemon control command using execEvil
     */
    private void execDaemonControlEvil(String daemonName, String command)
    {
        String cmd = (daemonName == null ? command : "/etc/init.d/" + daemonName + " " + command);        
        try {
            ExecManagerResultReader reader = UvmContextFactory.context().execManager().execEvil(cmd);
            reader.waitFor();
        }
        catch( Exception exn ) {
            logger.warn("Failed to run command: " + command, exn);
        }
    }

    /**
     * executes daemon control command using execOutput()
     * execOutput() is safer but the execManager can only run one at at time
     */
    private void execDaemonControlSafe(String daemonName, String command)
    {
        String cmd = (daemonName == null ? command : "/etc/init.d/" + daemonName + " " + command);
        String output = UvmContextFactory.context().execManager().execOutput(cmd);
        try {
            String lines[] = output.split("\\r?\\n");
            for (String line : lines)
                logger.info(cmd + ": " + line);
        } catch ( Exception exn ) {
        }
    }
    
    private void handleDaemonCheck(DaemonObject object)
    {
        synchronized( object ) {
            // run a spiffy command to count the number of process instances
            String result = UvmContextFactory.context().execManager().execOutput("pgrep " + object.searchString + " | wc -l");

            // parseInt is very finicky so we use replaceAll with
            // a regex to strip out anything that is not a digit 
            int count = Integer.parseInt(result.replaceAll("[^0-9]", ""));
            logger.debug("Found " + count + " instances of daemon/search: \"" + object.searchString + "\"");
        
            // if we find the process running just return
            if (count > 0)
                return;

            // process does not seem to be running so log and restart
            logger.warn("Found " + count + " instances of daemon/search: \"" + object.searchString + "\"");
            logger.warn("Restarting failed daemon: " + object.daemonName);
            execDaemonControlSafe(object.daemonName, "restart");
            
            if (object.extraRestartCommand != null) {
                try {
                    Thread.sleep(object.extraRestartDelay);
                }
                catch (Exception exn) {
                }
                execDaemonControlSafe(null,object.extraRestartCommand);
            }
        }
    }

    private void handleRequestCheck(DaemonObject object)
    {
        synchronized( object ) {
            DataOutputStream txstream = null;
            DataInputStream rxstream = null;
            Socket socket = null;
            boolean restart = false;
            byte buffer[] = new byte[4096];
            int txcount = 0;
            int rxcount = 0;

            // connect, send the command, and get the response using the old
            // school socket stuff so we use short timeout values which is
            // important since the stuff we monitor may be dead or unresponsive

            try {
                InetSocketAddress address = new InetSocketAddress(object.hostString, object.hostPort);
                socket = new Socket();
                socket.connect(address, 1000);
                socket.setSoTimeout(1000);
                txstream = new DataOutputStream(socket.getOutputStream());
                rxstream = new DataInputStream(socket.getInputStream());
                txstream.writeBytes(object.transmitString);
                txcount = txstream.size();
                rxcount = rxstream.read(buffer);
            }

            // catch and log any exceptions and set the restart flag
            catch ( Exception exn ) {
                String reason = exn.getMessage();
                if (reason == null && exn.getCause() != null)
                    reason = exn.getCause().toString();
                if (reason == null && exn.getClass() != null)
                    reason = exn.getClass().toString();
                if (reason == null)
                    reason = "unknown";

                logger.warn("Exception (" + reason + ") while checking " + object.hostString + ":" + object.hostPort);
                restart = true;
            }

            // make sure the streams and socket all get closed ignoring exceptions
            try {
                if (txstream != null)
                    txstream.close();
                if (rxstream != null)
                    rxstream.close();
                if (socket != null)
                    socket.close();
            } catch ( Exception exn ) {
            }

            // if no exceptions then we check the response for the search string
            if (restart == false) {
                String haystack = new String(buffer, 0, rxcount);
                if (haystack.contains(object.searchString) == false)
                    restart = true;
            }

            // if restart is we can just return
            if (restart == false)
                return;

            logger.warn("Restarting failed daemon: " + object.daemonName);
            execDaemonControlSafe(object.daemonName, "restart");

            if (object.extraRestartCommand != null) {
                try {
                    Thread.sleep(object.extraRestartDelay);
                }
                catch ( Exception exn ) {
                }
                execDaemonControlSafe(null,object.extraRestartCommand);
            }
        }
    }
}
