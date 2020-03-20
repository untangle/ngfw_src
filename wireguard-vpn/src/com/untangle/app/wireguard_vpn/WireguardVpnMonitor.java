/**
 * $Id$
 */

package com.untangle.app.wireguard_vpn;

import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.time.Instant;
import java.net.InetAddress;

import org.json.JSONTokener;
import org.json.JSONObject;
import org.json.JSONString;
import org.json.JSONArray;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;

/**
 * Class to monitor Wireguard tunnels. Tasks include collecting traffic
 * statistics, generating tunnel state transition events, and handling the ping
 * test when configured for a tunnel.
 */
class WireguardVpnMonitor implements Runnable
{
    /**
     * Define an object we can use to keep track of each Wireguard tunnel.
     */
    class TunnelWatcher
    {
        String publicKey;
        long rxLast;
        long txLast;
        long lastPing;
        long lastUpdate;

        /**
         * Constructor
         *
         * @param publicKey
         *        The public key of the tunnel
         */
        public TunnelWatcher(String publicKey)
        {
            this.publicKey = publicKey;
            rxLast = 0;
            txLast = 0;
            lastPing = 0;
            lastUpdate = 0;
        }
    }

    /* Delay a second while the thread is joining */
    private static final long THREAD_JOIN_TIME_MSEC = 1000;

    protected final Logger logger = Logger.getLogger(getClass());
    private final WireguardVpnApp app;

    private Hashtable<String, TunnelWatcher> watchTable = new Hashtable<>();
    private Thread thread = null;
    private volatile boolean isAlive = false;

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
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                logger.info("WireguardVpn monitor was interrupted");
            }

            if (!isAlive) break;

            /* call the main monitor worker function */
            monitorWorker();
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
     * Main worker function
     *
     */
    private void monitorWorker()
    {
        JSONTokener jsonTokener;
        JSONObject jsonObject;
        JSONArray statusList;

        // get the status of all tunnels
        String tunnelStatus = app.getTunnelStatus();

        // create a tokener from the tunnel status string
        try {
            jsonTokener = new JSONTokener(tunnelStatus);
        } catch (Exception exn) {
            logger.warn("Exception creating JSONTokener:", exn);
            return;
        }

        // create an object from the tokener
        try {
            jsonObject = new JSONObject(jsonTokener);
        } catch (Exception exn) {
            logger.warn("Exception creating JSONObject:", exn);
            return;
        }

        // create an array from the wireguard info in the object
        try {
            statusList = jsonObject.getJSONArray("wireguard");
        } catch (Exception exn) {
            logger.warn("Exception creating JSONArray:", exn);
            return;
        }

        // walk through the list of tunnel status records
        for (int x = 0; x < statusList.length(); x++) {
            JSONObject status;
            String peerkey;

            try {
                // get the tunnel status object at the current index
                status = statusList.getJSONObject(x);
            } catch (Exception exn) {
                logger.warn("Error creating status array:", exn);
                continue;
            }
            try {
                // get the peer key from the status record
                peerkey = status.getString("peer-key");
            } catch (Exception exn) {
                logger.warn("Error getting peer-key:", exn);
                continue;
            }

            // get the WireguardVpnTunnel tunnel for the peer key
            WireguardVpnTunnel tunnel = findTunnelByPublicKey(peerkey);
            if (tunnel == null) {
                logger.warn("Missing tunnel for " + peerkey);
                continue;
            }

            // get the TunnelWatcher entry for the peer key
            TunnelWatcher watcher = watchTable.get(peerkey);

            if (watcher == null) {
                logger.debug("Creating new watch table entry for " + peerkey);
                watcher = new TunnelWatcher(peerkey);
                watchTable.put(peerkey, watcher);
            } else {
                logger.debug("Found existing watch table entry for " + peerkey);
            }

            generateTunnelStatistics(tunnel, watcher, status);
            handleTunnelPingCheck(tunnel, watcher);
        }

        Set<String> keylist = watchTable.keySet();
        long nowtime = Instant.now().getEpochSecond();

        // look for stale entries in the watch table
        for (String key : keylist) {
            TunnelWatcher watcher = watchTable.get(key);

            // continue if the entry has been updated in the last hour
            if (nowtime < watcher.lastUpdate + 3600) continue;

            // entry has not been touched in a while so remove from the table
            logger.debug("Removing stale watchTable entry for " + watcher.publicKey);
            watchTable.remove(key);
        }
    }

    /**
     * Function to generate traffic statistics for a wireguard vpn tunnel
     *
     * @param tunnel
     *        - The tunnel object
     * @param watcher
     *        - The watcher object
     * @param status
     *        - The status object
     */
    private void generateTunnelStatistics(WireguardVpnTunnel tunnel, TunnelWatcher watcher, JSONObject status)
    {
        long inValue = 0;
        long outValue = 0;
        long inBytes = 0;
        long outBytes = 0;

        try {
            inValue = status.getLong("transfer-rx");
        } catch (Exception exn) {
            logger.warn("Error getting transfer-rx", exn);
            return;
        }

        try {
            outValue = status.getLong("transfer-tx");
        } catch (Exception exn) {
            logger.warn("Error getting transfer-tx");
            return;
        }

        // set the lastUpdate which is used by the table cleanup logic
        watcher.lastUpdate = Instant.now().getEpochSecond();

        // if neither value changed there is nothing to log so just return
        if ((inValue == watcher.rxLast) && (outValue == watcher.txLast)) return;

        /*
         * We calculate the rx and tx bytes since the last time we checked. If
         * the current values are less than we saw during the previous check we
         * assume the tunnel or counters were somehow reset and just use the new
         * the values as the raw byte counts for this iteration.
         */

        if (inValue < watcher.rxLast) inBytes = inValue;
        else inBytes = (inValue - watcher.rxLast);

        if (outValue < watcher.txLast) outBytes = outValue;
        else outBytes = (outValue - watcher.txLast);

        watcher.rxLast = inValue;
        watcher.txLast = outValue;

        WireguardVpnStats event = new WireguardVpnStats(tunnel.getDescription(), tunnel.getPeerAddress(), inBytes, outBytes);
        app.logEvent(event);
        logger.debug("GrabTunnelStatistics(logEvent) " + event.toString());
    }

    /**
     * Function to perform ping test for a wireguard vpn tunnel
     *
     * @param tunnel
     *        - The tunnel object
     * @param watcher
     *        - The watcher object
     */
    private void handleTunnelPingCheck(WireguardVpnTunnel tunnel, TunnelWatcher watcher)
    {
        // nothing to do if tunnel has no ping interval or address
        if (tunnel.getPingInterval() == 0) return;
        if (tunnel.getPingAddress() == null) return;
        if (tunnel.getPingAddress().length() == 0) return;

        // check the tunnel ping interval and return if below 
        long nowtime = Instant.now().getEpochSecond();
        if (nowtime < watcher.lastPing + tunnel.getPingInterval()) return;

        // update the last ping time
        watcher.lastPing = nowtime;

        try {
            InetAddress target = InetAddress.getByName(tunnel.getPingAddress());
            if (target.isReachable(2000)) {
                logger.debug("PING SUCCESS: " + tunnel.getPingAddress());
                return;
            }
        } catch (Exception exn) {
            logger.debug("PING EXCEPTION: " + tunnel.getPingAddress(), exn);
        }
        WireguardVpnEvent event = new WireguardVpnEvent(tunnel.getDescription(), WireguardVpnEvent.EventType.UNREACHABLE);
        app.logEvent(event);
        logger.debug("logEvent(wireguard_vpn_events) " + event.toSummaryString());
    }

    /**
     * Function to find the WireguardVpnTunnel in the application settings that
     * matches a given public key.
     *
     * @param finder
     *        The public key of the tunnel to find
     *
     * @return The WireguardVpnTunnel if found, otherwise null
     */
    private WireguardVpnTunnel findTunnelByPublicKey(String finder)
    {
        List<WireguardVpnTunnel> configList = app.getSettings().getTunnels();

        for (int x = 0; x < configList.size(); x++) {
            WireguardVpnTunnel tunnel = configList.get(x);
            if (tunnel.getPublicKey().equals(finder)) return (tunnel);
        }

        return (null);
    }
}
