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
import java.io.File;

import org.apache.log4j.Logger;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.PlatformManager;
import com.untangle.uvm.HookCallback;
import com.untangle.uvm.network.NetworkSettings;

/**
 * This class provides a common place and mechanisms for performing periodic
 * tasks, and setting callback hooks for managing resources that are shared with
 * all the applications and services on the platform.
 * 
 * @author mahotz
 * 
 */

public class PlatformManagerImpl extends TimerTask implements PlatformManager
{
    private final Logger logger = Logger.getLogger(getClass());
    private final Timer timer = new Timer("PlatformManager", true);

    private static final String PLATFORM_HOSTS_UPDATE_SCRIPT = System.getProperty("uvm.home") + "/bin/ut-update-platform-hosts";
    private static final String PLATFORM_HOSTS_RELOAD_COMMAND = "killall -HUP dnsmasq";

    private PlatformManagerHookCallback platformManagerHookCallback;
    private long platformHostsFileTimestamp = 0;

    public PlatformManagerImpl()
    {
        platformManagerHookCallback = new PlatformManagerHookCallback(this);

        // schedule our fixed timer to handle periodic tasks
        timer.schedule(this, 1000, 60000);
        logger.info("The PlatformManager has been initialized.");

        // install a callback for network settings changes
        UvmContextFactory.context().hookManager().registerCallback(com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, platformManagerHookCallback);
    }

    /**
     * This function will be called periodically as schedule from our
     * constructor as well as being called directly from the network settings
     * change callback hook.
     */

    public synchronized void run()
    {
        if (logger.isDebugEnabled()) logger.debug("The PlatformManager timer task is starting");

        refreshPlatformHostsFile();

        if (logger.isDebugEnabled()) logger.debug("The PlatformManager timer task has finished");
    }

    /**
     * This class implements our network settings change hook callback. It will
     * be called whenever network settings are changed. In response we will call
     * the run function of our main platform manager.
     */
    private class PlatformManagerHookCallback implements HookCallback
    {
        private PlatformManagerImpl owner;

        PlatformManagerHookCallback(PlatformManagerImpl app)
        {
            this.owner = app;
        }

        public String getName()
        {
            return "platform-manager-network-settings-change-hook";
        }

        public void callback(Object o)
        {
            if (!(o instanceof NetworkSettings)) {
                logger.warn("Invalid network settings: " + o);
                return;
            }

            NetworkSettings settings = (NetworkSettings) o;
            if (logger.isDebugEnabled()) logger.debug("Network settings changed. Running platform tasks.");

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
    public void refreshPlatformHostsFile()
    {
        long mostRecentTimestamp = 0;
        int changeCounter = 0;

        if (logger.isDebugEnabled()) logger.debug("Scanning interface status files");

        try {
            File devFilePath = new File("/var/lib/untangle-netd");
            File[] devFileList = devFilePath.listFiles();

            for (int i = 0; i < devFileList.length; i++) {
                if (devFileList[i].lastModified() > platformHostsFileTimestamp) {
                    if (logger.isDebugEnabled()) logger.debug("New or updated file detected: " + devFileList[i].getAbsolutePath());
                    changeCounter += 1;
                    if (devFileList[i].lastModified() > mostRecentTimestamp) mostRecentTimestamp = devFileList[i].lastModified();
                }
            }
        } catch (Exception exn) {
            logger.warn("Exception reading interface status files", exn);
            platformHostsFileTimestamp = 0;
            return;
        }

        // if no status files have been added or changed we are finished
        if (changeCounter == 0) return;

        if (logger.isDebugEnabled()) logger.debug("Detected " + Integer.toString(changeCounter) + " new or modified interface status files. Updating platform hosts file");

        // changes detected so use the timestamp from the newest
        platformHostsFileTimestamp = mostRecentTimestamp;

        // call the script to update the platform hosts file
        UvmContextFactory.context().execManager().exec(PLATFORM_HOSTS_UPDATE_SCRIPT);

        // execute the command to activate the updated platform hosts file
        UvmContextFactory.context().execManager().exec(PLATFORM_HOSTS_RELOAD_COMMAND);
    }
}
