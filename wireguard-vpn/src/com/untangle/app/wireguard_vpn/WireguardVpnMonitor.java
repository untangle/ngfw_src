/**
 * $Id$
 */

package com.untangle.app.wireguard_vpn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;

/**
 * Monitors the Wireguard VPN daemon for active tunnels, capturing statistics and
 * automatically restarting any that terminate unexpectedly.
 * 
 * @author mahotz
 * 
 */
class WireguardVpnMonitor implements Runnable
{
    private static final long TRAFFIC_CHECK_INTERVAL = (60 * 1000);
    private static final long PROCESS_RESTART_DELAY = (30 * 1000);

    /* Delay a second while the thread is joining */
    private static final long THREAD_JOIN_TIME_MSEC = 1000;

    /* Interrupt if there is no traffic for 2 seconds */
    private static final int READ_TIMEOUT = 2000;

    private static final String STATE_CMD = "state";
    private static final String STATUS_CMD = "status";
    private static final String XMIT_MARKER = "TCP/UDP write bytes";
    private static final String RECV_MARKER = "TCP/UDP read bytes";
    private static final String END_MARKER = "end";

    protected final Logger logger = Logger.getLogger(getClass());

    private final ConcurrentHashMap<Integer, WireguardVpnTunnelStatus> tunnelStatusList = new ConcurrentHashMap<>();
    private final WireguardVpnApp app;

    private Thread thread = null;
    private volatile boolean isAlive = false;
    private volatile long cycleCount = 0;

    /**
     * Constructor
     * 
     * @param app
     *        The Tunnel VPN application
     */
    protected WireguardVpnMonitor(WireguardVpnApp app)
    {
        this.app = app;
    }

    /**
     * The main run function
     */
    public void run()
    {
        if (!isAlive) {
            logger.error("died before starting");
            return;
        }

        logger.debug("Starting");

        while (isAlive) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                logger.info("WireguardVpn monitor was interrupted");
            }

            if (!isAlive) break;

            /* check that all the enabled tunnels are running and active */
            checkTunnelProcesses();

            /* generate the status and traffic statistics for each tunnel */
            generateTunnelStatistics();
        }

        logger.debug("Finished");
    }

    /**
     * Called to start monitoring
     */
    public synchronized void start()
    {
        isAlive = true;

        logger.debug("Starting WireguardVpn monitor");

        /*
         * If thread is not-null, there is a running thread that thinks it is
         * alive
         */
        if (thread != null) {
            logger.debug("WireguardVpn monitor is already running");
            return;
        }

        thread = UvmContextFactory.context().newThread(this);
        thread.start();
    }

    /**
     * Called to stop monitoring
     */
    public synchronized void stop()
    {
        isAlive = false;

        if (thread != null) {
            logger.debug("Stopping WireguardVpn monitor");
            try {
                thread.interrupt();
                thread.join(THREAD_JOIN_TIME_MSEC);
            } catch (SecurityException e) {
                logger.error("security exception, impossible", e);
            } catch (InterruptedException e) {
                logger.error("interrupted while stopping", e);
            }
            thread = null;
        }
    }

    /**
     * Checks that all enabled tunnels have a running Wireguard VPN process. If any
     * are missing the process is restarted.
     */
    private void checkTunnelProcesses()
    {
        cycleCount += 1;

        // BufferedReader bufferedReader = null;
        // FileReader fileReader = null;
        // /*
        //  * Tunnel status objects that didn't get updated are left over from a
        //  * tunnel that was deleted so we clean them up here.
        //  */
        // for (Map.Entry<Integer, WireguardVpnTunnelStatus> entry : tunnelStatusList.entrySet()) {
        //     Integer key = entry.getKey();
        //     WireguardVpnTunnelStatus value = entry.getValue();
        //     if (value.getCycleCount() != cycleCount) tunnelStatusList.remove(value.getTunnelId());
        // }

    }

    /**
     * Connects to the wireguard vpn management port for every active tunnel to get
     * the connection and traffic stats. We also watch for transitions between
     * CONNECTED and DISCONNECTED and generate log events as required.
     */
    protected void generateTunnelStatistics()
    {
        WireguardVpnTunnelStatus status = null;
        WireguardVpnEvent event = null;

    }

    /**
     * Gets a list of the status for each active tunnel
     * 
     * @return The tunnel status list
     */
    public LinkedList<WireguardVpnTunnelStatus> getTunnelStatusList()
    {
        LinkedList<WireguardVpnTunnelStatus> statusList = new LinkedList<>();

        // if the app is not running just return an empty list
        if (app.getRunState() != AppSettings.AppState.RUNNING) {
            return (statusList);
        }

        checkTunnelProcesses();
        generateTunnelStatistics();

        for (Map.Entry<Integer, WireguardVpnTunnelStatus> entry : tunnelStatusList.entrySet()) {
            Integer key = entry.getKey();
            WireguardVpnTunnelStatus value = entry.getValue();
            statusList.add(value);
        }

        return (statusList);
    }

}
