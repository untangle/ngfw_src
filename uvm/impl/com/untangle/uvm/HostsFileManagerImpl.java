/**
 * $Id$
 */

package com.untangle.uvm;

import java.util.TimerTask;
import java.util.Timer;
import java.io.File;

import org.apache.log4j.Logger;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostsFileManager;
import com.untangle.uvm.HookCallback;
import com.untangle.uvm.network.NetworkSettings;

/**
 * This class uses a Timer and a NetworkSettings HookCallback to watch for
 * changes to any of the interface status files. When changes are detected, we
 * refresh /etc/hosts.untangle which is loaded by dnsmasq. This file contains an
 * IP to our configured hostname entry for every interface on the server. It was
 * originally created to suppor the Captive Portal redirect using hostname
 * instead of IP feature. The dnsmasq daemon is configured to give the best
 * matching IP in response to a client query based on the interface on which the
 * query was received.
 * 
 * @author mahotz
 * 
 */
public class HostsFileManagerImpl extends TimerTask implements HostsFileManager
{
    private final Logger logger = Logger.getLogger(getClass());
    private final Timer timer = new Timer("HostsFileManager", true);

    private static final String HOSTS_FILE_UPDATE_SCRIPT = System.getProperty("uvm.home") + "/bin/ut-update-hosts-file";
    private static final String HOSTS_FILE_RELOAD_COMMAND = "killall -HUP dnsmasq";

    private HostsFileManagerHookCallback hostsFileManagerHookCallback;
    private long latestTimestamp = 0;

    /**
     * Constructor
     */
    public HostsFileManagerImpl()
    {
        hostsFileManagerHookCallback = new HostsFileManagerHookCallback(this);

        // schedule our fixed timer to handle periodic tasks
        timer.schedule(this, 1000, 60000);
        logger.info("The HostsFileManager has been initialized.");

        // install a callback for network settings changes
        UvmContextFactory.context().hookManager().registerCallback(com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, hostsFileManagerHookCallback);
    }

    /**
     * This function will be called periodically as schedule from our
     * constructor as well as being called directly from the network settings
     * change callback hook.
     */
    public synchronized void run()
    {
        if (logger.isDebugEnabled()) logger.debug("The HostsFileManager timer task is starting");

        refreshHostsFile();

        if (logger.isDebugEnabled()) logger.debug("The HostsFileManager timer task has finished");
    }

    /**
     * This class implements our network settings change hook callback. It will
     * be called whenever network settings are changed. In response we will call
     * the same run function called by the Timer.
     */
    private class HostsFileManagerHookCallback implements HookCallback
    {
        private HostsFileManagerImpl owner;

        /**
         * Constructor
         * 
         * @param app
         *        The main application
         */
        HostsFileManagerHookCallback(HostsFileManagerImpl app)
        {
            this.owner = app;
        }

        /**
         * Get the name of our callback hook
         * 
         * @return The name of our callback hook
         */
        public String getName()
        {
            return "hosts-file-manager-network-settings-change-hook";
        }

        /**
         * Our callback function
         * 
         * @param args
         *        The arguments passed to the callback
         */
        public void callback(Object... args)
        {
            Object o = args[0];
            if (!(o instanceof NetworkSettings)) {
                logger.warn("Invalid network settings: " + o);
                return;
            }

            NetworkSettings settings = (NetworkSettings) o;
            if (logger.isDebugEnabled()) logger.debug("Network settings changed. Calling update function.");

            // call our timer run function to execute everything immediately
            owner.run();
        }
    }

    /**
     * This function will monitor all of the interface status files, and
     * automatically update /etc/hosts.untangle and HUP the dnsmasq daemon when
     * any changes are detected. This supports the Captive Portal redirect using
     * hostname instead of IP address feature by ensuring our configured
     * hostname resolves to an appropriate IP address based on the interface
     * where the client traffic was received.
     */
    public void refreshHostsFile()
    {
        long workingTimestamp = 0;
        int changeCounter = 0;

        if (logger.isDebugEnabled()) logger.debug("Scanning interface status files");

        try {
            File devFilePath = new File("/var/lib/interface-status");
            File[] devFileList = devFilePath.listFiles();

            // if the directory doesn't exist we are finished
            if (devFileList == null) return;

            for (int i = 0; i < devFileList.length; i++) {
                if (devFileList[i].lastModified() > latestTimestamp) {
                    if (logger.isDebugEnabled()) logger.debug("New or updated file detected: " + devFileList[i].getAbsolutePath());
                    changeCounter += 1;
                    if (devFileList[i].lastModified() > workingTimestamp) workingTimestamp = devFileList[i].lastModified();
                }
            }
        } catch (Exception exn) {
            logger.warn("Exception reading interface status files", exn);
            latestTimestamp = 0;
            return;
        }

        // if no status files have been added or changed we are finished
        if (changeCounter == 0) return;

        logger.info("Detected " + Integer.toString(changeCounter) + " new or modified interface status files. Updating hosts file");

        // changes detected so use the timestamp from the newest
        latestTimestamp = workingTimestamp;

        String fullName = UvmContextFactory.context().networkManager().getFullyQualifiedHostname();

        // call the script to update the hosts file
        UvmContextFactory.context().execManager().exec(HOSTS_FILE_UPDATE_SCRIPT + " " + fullName);

        // execute the command to activate the updated hosts file
        UvmContextFactory.context().execManager().exec(HOSTS_FILE_RELOAD_COMMAND);
    }
}
