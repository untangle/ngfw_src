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

    /**
     * Holds the details of a daemon that is controlled and monitored by the
     * daemon manager.
     */
    class DaemonObject
    {
        /**
         * Constructor
         * 
         * @param daemonName
         *        The daemon name
         */
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
    private ConcurrentHashMap<String, DaemonObject> daemonHashMap = new ConcurrentHashMap<>();

    /**
     * Constructor
     */
    public DaemonManagerImpl()
    {
        // we'll do the monitoring stuff every 5 seconds 
        timer.scheduleAtFixedRate(this, 5000, 5000);
    }

    /**
     * The main thread function for the daemon manager
     */
    public void run()
    {
        for (DaemonObject object : daemonHashMap.values()) {
            // ignore objects not configured for monitoring
            if (object.monitorType == MonitorType.DISABLED) continue;

            // ignore objects that don't need to be checked yet
            if (System.currentTimeMillis() < (object.lastCheck + object.monitorInterval)) continue;

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

    /**
     * Gets the usage count for a particular daemon
     * 
     * @param daemonName
     *        The daemon name
     * @return The usage count
     */
    public int getUsageCount(String daemonName)
    {
        DaemonObject daemonObject = getDaemonObject(daemonName);
        return daemonObject.usageCount;
    }

    /**
     * Return status from systemctl in machine readable format
     *
     * @param daemonName
     *        The daemon name
     * @return String of systemctl status.
     */
    public String getStatus(String daemonName)
    {
        return execDaemonControlSafe(daemonName, "show", false);
    }

    /**
     * Increments the usage count for a particular daemon
     *
     * @param daemonName
     *        The daemon name
     */
    public void incrementUsageCount(String daemonName)
    {
        DaemonObject daemonObject = getDaemonObject(daemonName);
        synchronized (daemonObject) {
            int newUsageCount = daemonObject.usageCount + 1;
            daemonObject.usageCount = newUsageCount;

            if (newUsageCount == 1) {
                execDaemonControlEvil(daemonName, "start");
            }
        }
    }

    /**
     * Decrements the usage count for a particular daemon
     *
     * @param daemonName
     *        The daemon name
     */
    public void decrementUsageCount(String daemonName)
    {
        DaemonObject daemonObject = getDaemonObject(daemonName);
        synchronized (daemonObject) {
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

    /**
     * Send configuration reload command to daemon.
     *
     * @param daemonName
     *        The daemon name
     */
    public void reload(String daemonName)
    {
        DaemonObject daemonObject = getDaemonObject(daemonName);
        synchronized (daemonObject) {
            execDaemonControlEvil(daemonName, "reload");
        }
    }

    /**
     * Used to set an optional additional command that should be executed after
     * the normal restart command is executed.
     * 
     * @param daemonName
     *        The daemon command
     * @param extraRestartCommand
     *        The command to be executed
     * @param extraRestartDelay
     *        The amount of time to wait after executing the standard restart
     *        before executing the extra command
     */
    public void setExtraRestartCommand(String daemonName, String extraRestartCommand, long extraRestartDelay)
    {
        DaemonObject daemonObject = getDaemonObject(daemonName);
        synchronized (daemonObject) {
            daemonObject.extraRestartCommand = extraRestartCommand;
            daemonObject.extraRestartDelay = extraRestartDelay;
        }
    }

    /**
     * Called to enable simple daemon monitoring where we look to see if the
     * daemon is running by periodically calling pgrep using the argumented
     * search string.
     * 
     * @param daemonName
     *        The daemon name
     * @param secondInterval
     *        The number of seconds between checks
     * @param searchString
     *        The search string to use
     * @return True if the monitor is successfully applied, false if no daemon
     *         with the argumented name was found
     */
    public boolean enableDaemonMonitoring(String daemonName, long secondInterval, String searchString)
    {
        DaemonObject daemonObject = daemonHashMap.get(daemonName);
        if (daemonObject == null) return (false);

        daemonObject.monitorType = MonitorType.DAEMON;
        daemonObject.monitorInterval = (secondInterval * 1000);
        daemonObject.searchString = searchString;
        daemonObject.lastCheck = System.currentTimeMillis();
        return (true);
    }

    /**
     * Called to enable more comprehensive monitoring where we connect to the
     * daemon at a specific host:port combination, transmit a string, recive the
     * response, and search the response for a particular string to determine if
     * the daemon is running and responding properly.
     * 
     * @param daemonName
     *        The daemon name
     * @param secondInterval
     *        The number of seconds between checks
     * @param hostString
     *        The target host
     * @param hostPort
     *        The target port
     * @param transmitString
     *        The string to transmit
     * @param searchString
     *        The response search string
     * @return True if the monitor is successfully applied, false if no daemon
     *         with the argumented name was found
     */
    public boolean enableRequestMonitoring(String daemonName, long secondInterval, String hostString, int hostPort, String transmitString, String searchString)
    {
        DaemonObject daemonObject = daemonHashMap.get(daemonName);
        if (daemonObject == null) return (false);

        daemonObject.monitorType = MonitorType.REQUEST;
        daemonObject.monitorInterval = (secondInterval * 1000);
        daemonObject.hostString = hostString;
        daemonObject.hostPort = hostPort;
        daemonObject.transmitString = transmitString;
        daemonObject.searchString = searchString;
        daemonObject.lastCheck = System.currentTimeMillis();
        return (true);
    }

    /**
     * Called to disable all monitoring for a daemon.
     * 
     * @param daemonName
     *        The daemon name
     * @return True if all monitoring is successfully disabled, false if no
     *         daemon with the argumented name was found
     */
    public boolean disableAllMonitoring(String daemonName)
    {
        DaemonObject daemonObject = daemonHashMap.get(daemonName);
        if (daemonObject == null) return (false);

        daemonObject.transmitString = null;
        daemonObject.searchString = null;
        daemonObject.monitorInterval = 0;
        daemonObject.lastCheck = 0;
        daemonObject.hostString = null;
        daemonObject.hostPort = 0;
        daemonObject.monitorType = MonitorType.DISABLED;
        return (true);
    }

    /**
     * Checks to see if a daemon is running. If monitoring is enabled, we search
     * for the searchString associated with the DaemonObject, otherwise we just
     * look for the daemon name.
     * 
     * @param daemonName
     *        The daemon name
     * @return True if the daemon is running, otherwise false
     */
    public boolean isRunning(String daemonName)
    {
        DaemonObject daemonObject = daemonHashMap.get(daemonName);
        if (daemonObject == null) return (false);

        String daemonSearchString = ( (daemonObject.monitorType == MonitorType.DAEMON && daemonObject.searchString != null) ? daemonObject.searchString : daemonName);
        daemonSearchString = "[" + daemonSearchString.substring(0,1) + "]" + daemonSearchString.substring(1);
        String result = UvmContextFactory.context().execManager().execOutput("ps -e -o command h | cut -f1 -d' ' | grep " + daemonSearchString + " | wc -l");
        return ( Integer.parseInt(result.trim()) == 1 ) ? true : false;
    }

    /**
     * Gets the DaemonObject with the argumented name from the hash map. If the
     * daemon object doesn't yet exist, it is created.
     * 
     * @param daemonName
     *        The daemon name
     * @return The corresponding DaemonObject
     */
    private synchronized DaemonObject getDaemonObject(String daemonName)
    {
        DaemonObject daemonObject = daemonHashMap.get(daemonName);

        if (daemonObject == null) {
            daemonObject = new DaemonObject(daemonName);
            daemonHashMap.put(daemonName, daemonObject);
        }

        return daemonObject;
    }

    /**
     * Executes a daemon control command using execEvil. If the daemon name is
     * provided, we exec systemctl passing the command and the daemonName. If
     * daemonName is null, we simply exec the command as provided.
     * 
     * @param daemonName
     *        The daemon name
     * @param command
     *        The systemctl command to execute, such as start, stop, restart,
     *        etc. or the raw command to execute if daemonName is null
     */
    private void execDaemonControlEvil(String daemonName, String command)
    {
        String cmd = (daemonName == null ? command : "systemctl " + command + " " + daemonName);
        try {
            ExecManagerResultReader reader = UvmContextFactory.context().execManager().execEvil(cmd);
            reader.waitFor();
        } catch (Exception exn) {
            logger.warn("Failed to run command: " + command, exn);
        }
    }

    /**
     * Executes daemon control command using execOutput() which is safer but the
     * execManager can only run one at at time. If the daemon name is provided,
     * we exec systemctl passing the command and the daemonName. If daemonName
     * is null, we simply exec the command as provided.
     *
     * @param daemonName
     *        The daemon name
     * @param command
     *        The systemctl command to execute, such as start, stop, restart,
     *        etc. or the raw command to execute if daemonName is null
     */
    private void execDaemonControlSafe(String daemonName, String command){
        execDaemonControlSafe(daemonName, command, true);
    }

    /**
     * Executes daemon control command using execOutput() which is safer but the
     * execManager can only run one at at time. If the daemon name is provided,
     * we exec systemctl passing the command and the daemonName. If daemonName
     * is null, we simply exec the command as provided.
     *
     * @param daemonName
     *        The daemon name
     * @param command
     *        The systemctl command to execute, such as start, stop, restart,
     *        etc. or the raw command to execute if daemonName is null
     * @param log
     *        If true, log output.
     * @return String of command output.
     */
    private String execDaemonControlSafe(String daemonName, String command, boolean log)
    {
        String cmd = (daemonName == null ? command : "systemctl " + command + " " + daemonName);
        String output = UvmContextFactory.context().execManager().execOutput(cmd);
        if(log){
            try {
                String lines[] = output.split("\\r?\\n");
                for (String line : lines)
                    logger.info(cmd + ": " + line);
            } catch (Exception exn) {
            }
        }
        return output;
    }

    /**
     * Handles checking the status of daemons that have basic process monitoring
     * enabled, and restarting if the process if found to not be running.
     * 
     * @param object
     *        The DaemonObject to check
     */
    private void handleDaemonCheck(DaemonObject object)
    {
        synchronized (object) {
            // run a spiffy command to count the number of process instances
            String result = UvmContextFactory.context().execManager().execOutput("pgrep " + object.searchString + " | wc -l");

            // parseInt is very finicky so we use replaceAll with
            // a regex to strip out anything that is not a digit 
            int count = Integer.parseInt(result.replaceAll("[^0-9]", ""));
            logger.debug("Found " + count + " instances of daemon/search: \"" + object.searchString + "\"");

            // if we find the process running just return
            if (count > 0) return;

            // process does not seem to be running so log and restart
            logger.warn("Found " + count + " instances of daemon/search: \"" + object.searchString + "\"");
            logger.warn("Restarting failed daemon: " + object.daemonName);
            execDaemonControlSafe(object.daemonName, "restart");

            if (object.extraRestartCommand != null) {
                try {
                    object.wait(object.extraRestartDelay);
                } catch (Exception exn) {
                }
                execDaemonControlSafe(null, object.extraRestartCommand);
            }
        }
    }

    /**
     * Handles checking the status of daemons that have request monitoring
     * enabled, and restarting if the daemon is not responding as expected.
     * 
     * @param object
     *        The DaemonObject to check
     */
    private void handleRequestCheck(DaemonObject object)
    {
        synchronized (object) {
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
            }catch (Exception exn) {
                // catch and log any exceptions and set the restart flag
                String reason = exn.getMessage();
                if (reason == null && exn.getCause() != null) reason = exn.getCause().toString();
                if (reason == null && exn.getClass() != null) reason = exn.getClass().toString();
                if (reason == null) reason = "unknown";

                logger.warn("Exception (" + reason + ") while checking " + object.hostString + ":" + object.hostPort);
                restart = true;
            } finally {
                // make sure the streams and socket all get closed ignoring exceptions
                if(socket != null){
                    try{
                        socket.close();
                    }catch(Exception e){
                        logger.warn(e);
                    }
                }
                if(txstream != null){
                    try{
                        txstream.close();
                    }catch(Exception e){
                        logger.warn(e);
                    }
                }
                if(rxstream != null){
                    try{
                        rxstream.close();
                    }catch(Exception e){
                        logger.warn(e);
                    }
                }
            }

            // if no exceptions then we check the response for the search string
            if (restart == false) {
                String haystack = new String(buffer, 0, rxcount);
                if (haystack.contains(object.searchString) == false) restart = true;
            }

            // if restart is we can just return
            if (restart == false) return;

            logger.warn("Restarting failed daemon: " + object.daemonName);
            execDaemonControlSafe(object.daemonName, "restart");

            if (object.extraRestartCommand != null) {
                try {
                    object.wait(object.extraRestartDelay);
                } catch (Exception exn) {
                }
                execDaemonControlSafe(null, object.extraRestartCommand);
            }
        }
    }
}
