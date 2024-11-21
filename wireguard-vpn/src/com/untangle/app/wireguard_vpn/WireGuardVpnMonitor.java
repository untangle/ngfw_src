/**
 * $Id$
 */

package com.untangle.app.wireguard_vpn;

import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.time.Instant;
import java.net.InetAddress;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.FileReader;

import org.json.JSONTokener;
import org.json.JSONObject;
import org.json.JSONString;
import org.json.JSONArray;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.untangle.uvm.HostTableEntry;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;

/**
 * Class to monitor WireGuard tunnels. Tasks include collecting traffic
 * statistics, generating tunnel state transition events, and handling the ping
 * test when configured for a tunnel.
 */
class WireGuardVpnMonitor implements Runnable
{
    /**
     * Define an object we can use to keep track of each WireGuard tunnel.
     */
    class TunnelWatcher
    {
        String publicKey;
        long lastUpdateTime;
        long lastRxBytes;
        long lastTxBytes;
        long lastPingTime;
        boolean virtualStateFlag;

        /**
         * Constructor
         * 
         * @param publicKey
         *        The public key of the tunnel
         */
        public TunnelWatcher(String publicKey)
        {
            this.publicKey = publicKey;
            lastUpdateTime = 0;
            lastRxBytes = 0;
            lastTxBytes = 0;
            lastPingTime = 0;
            virtualStateFlag = false;
        }
    }

    private static final long THREAD_JOIN_TIME_MSEC = 1000; // milliseconds delay to allow thread join when shutting down
    private static final long TUNNEL_ACTIVITY_TIMEOUT = 60; // seconds to wait before considering a tunnel down

    protected final Logger logger = LogManager.getLogger(getClass());
    private final WireGuardVpnApp app;

    private Hashtable<String, TunnelWatcher> watchTable = new Hashtable<>();
    private Thread thread = null;
    private volatile boolean isAlive = false;

    /**
     * Constructor
     *
     * @param app
     *        The Tunnel VPN application
     */
    protected WireGuardVpnMonitor(WireGuardVpnApp app)
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
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                logger.info("WireGuardVpn monitor was interrupted");
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

        logger.debug("Starting WireGuardVpn monitor");

        /*
         * If thread is not-null, there is a running thread that thinks it is
         * alive
         */
        if (thread != null) {
            logger.debug("WireGuardVpn monitor is already running");
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
            logger.debug("Stopping WireGuardVpn monitor");
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
     * Main worker function gets wireguard and raw interface stats for all
     * devices and then calls worker functions for all configured tunnels.
     *
     */
    private void monitorWorker()
    {
        BufferedReader reader = null;
        JSONTokener jsonTokener;
        JSONObject jsonObject;
        JSONArray statusList;

        /*
         * Now get the status details for all wireguard tunnels
         */
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

            // get the WireGuardVpnTunnel tunnel for the peer key
            WireGuardVpnTunnel tunnel = findTunnelByPublicKey(peerkey);
            if (tunnel == null) {
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
            if (nowtime < (watcher.lastUpdateTime + 3600)) continue;

            // entry has not been touched in a while so remove from the table
            logger.debug("Removing stale watchTable entry for " + watcher.publicKey);
            //Removing hosttable entry for tunnel, as entry is removed above
            WireGuardVpnTunnel tunnel = findTunnelByPublicKey(key);
            removeHostTableEntry(tunnel);

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
    private void generateTunnelStatistics(WireGuardVpnTunnel tunnel, TunnelWatcher watcher, JSONObject status)
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

        /*
         * If this is the first time checking this tunnel we need to use the
         * current RX and TX values as the starting point for delta caculations.
         */
        if (watcher.lastUpdateTime == 0) {
            watcher.lastUpdateTime = Instant.now().getEpochSecond();
            watcher.lastRxBytes = inValue;
            watcher.lastTxBytes = outValue;
            //Adding description field in Username and Hostname of HostTableEntry table
            addHostTableEntry(tunnel, true);

            return;
        }

        // set the lastUpdateTime which is used by the table cleanup logic
        watcher.lastUpdateTime = Instant.now().getEpochSecond();

        // if neither value changed there is nothing to log so just return
        if ((inValue == watcher.lastRxBytes) && (outValue == watcher.lastTxBytes)) return;

        /*
         * We calculate the rx and tx bytes since the last time we checked. If
         * the current values are less than we saw during the previous check we
         * assume the tunnel or counters were somehow reset and just use the new
         * the values as the raw byte counts for this iteration.
         */

        if (inValue < watcher.lastRxBytes) inBytes = inValue;
        else inBytes = (inValue - watcher.lastRxBytes);

        if (outValue < watcher.lastTxBytes) outBytes = outValue;
        else outBytes = (outValue - watcher.lastTxBytes);

        /*
         * as per wireguard documentation,  (multiples of 148) bytes sent for handshake after every 2 minutes in outBytes,
         * if handshake is successful 92 bytes recieved
         * in case of failure 0 bytes in inBytes, skipping in case of failure.
         */
        if ((inBytes == 0) && (outBytes % 148 == 0)) {
            logger.debug("Wireguard tunnel handshake condition inBytes=" + inBytes + ", outBytes=" + outBytes);
            return;
        }

        watcher.lastRxBytes = inValue;
        watcher.lastTxBytes = outValue;

        WireGuardVpnStats event = new WireGuardVpnStats(tunnel.getDescription(), tunnel.getPeerAddress(), inBytes, outBytes);
        app.logEvent(event);
        /* For roaming clients there are no connection events, hence we are updating HostTable entry here as in case of successful tunnel connection bytes will be transferred/recieved,
         * Incase of reconnects This will ensure HostTable entry is updated, code in HostTable first checks the value if it is same, then update is skipped.
         *
         *
         *
         */
        if (tunnel.getEndpointDynamic()) {
            addHostTableEntry(tunnel, false);
        }
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
    private void handleTunnelPingCheck(WireGuardVpnTunnel tunnel, TunnelWatcher watcher)
    {
        boolean pingSuccess = false;

        // nothing to do if tunnel is roaming or has no ping interval or address
        if (tunnel.getEndpointDynamic()) return;
        if (tunnel.getPingInterval() == 0) return;
        if (tunnel.getPingAddress() == null) return;

        // check the tunnel ping interval and return if below
        long nowtime = Instant.now().getEpochSecond();
        if (nowtime < (watcher.lastPingTime + tunnel.getPingInterval())) return;

        // update the last ping time
        watcher.lastPingTime = nowtime;

        try {
            InetAddress target = InetAddress.getByName(tunnel.getPingAddress().getHostAddress());
            if (target.isReachable(2000)) {
                pingSuccess = true;
            }
        } catch (Exception exn) {
            logger.debug("PING EXCEPTION: " + tunnel.getPingAddress().getHostAddress(), exn);
        }

        if (pingSuccess == false) {
            // if ping unreachable events are enabled log an unreachable event
            if (tunnel.getPingUnreachableEvents()) {
                WireGuardVpnEvent event = new WireGuardVpnEvent(tunnel.getDescription(), WireGuardVpnEvent.EventType.UNREACHABLE);
                app.logEvent(event);
                logger.debug("logEvent(wireguard_vpn_events) " + event.toSummaryString());
                //Removing HostTable entry during Unreachable event
                removeHostTableEntry(tunnel);

            }

            // if connection events are not enabled just return
            if (tunnel.getPingConnectionEvents() == false) return;

            // if the virtual state flag is already clear just return
            if (watcher.virtualStateFlag == false) return;

            // clear the virtual state flag, log a disconnect event, and return
            watcher.virtualStateFlag = false;
            WireGuardVpnEvent event = new WireGuardVpnEvent(tunnel.getDescription(), WireGuardVpnEvent.EventType.DISCONNECT);
            app.logEvent(event);
            logger.debug("logEvent(wireguard_vpn_events) " + event.toSummaryString());
            //Removing HostTable entry during Disconnect event
            removeHostTableEntry(tunnel);
            return;
        }

        // if the virtual state flag is already set just return
        if (watcher.virtualStateFlag == true) return;

        // set the virtual state flag and log a connect event
        watcher.virtualStateFlag = true;
        WireGuardVpnEvent event = new WireGuardVpnEvent(tunnel.getDescription(), WireGuardVpnEvent.EventType.CONNECT);
        //Updating HostTable entry during connect event
        addHostTableEntry(tunnel, false);
        app.logEvent(event);
        logger.debug("logEvent(wireguard_vpn_events) " + event.toSummaryString());
    }

    /**
     * Function to add or update HostTable entry for particular Wireguard Peer Address.
     *
     * @param tunnel
     *        The tunnel's information to be deleted from HostTableEntry
     *
     */
    private void removeHostTableEntry(WireGuardVpnTunnel tunnel)
    {
        if (tunnel != null && tunnel.getPeerAddress() != null) {
            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(tunnel.getPeerAddress(), false);
            if (entry != null) {
                //Update username when Tunnel connection is Disconnected or Unreachable
                entry.setUsernameWireGuardVpn(null);
            }
        }
    }

    /**
     * Function to remove HostTable entry for particular Wireguard Peer Address.
     *
     * @param tunnel
     *        The tunnel's information to be added to HostTableEntry
     * @param createIfNecessary
     *        True to create if it doesn't exist
     */
    private void addHostTableEntry(WireGuardVpnTunnel tunnel, Boolean createIfNecessary)
    {
        boolean isMappingEnabled = app.getSettings().isMapTunnelDescUser();
        if (isMappingEnabled && tunnel != null && tunnel.getPeerAddress() != null) {
            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(tunnel.getPeerAddress(), createIfNecessary);
                entry.setHostnameWireGuardVpn(tunnel.getDescription());
                //Update username when Tunnel connection is established
                if (!createIfNecessary) {
                    entry.setUsernameWireGuardVpn(tunnel.getDescription());
                } else {
                    entry.setUsernameWireGuardVpn(null);
                }

        }
    }

    /**
     * Function to find the WireGuardVpnTunnel in the application settings that
     * matches a given public key.
     *
     * @param finder
     *        The public key of the tunnel to find
     *
     * @return The WireGuardVpnTunnel if found, otherwise null
     */
    private WireGuardVpnTunnel findTunnelByPublicKey(String finder)
    {
        List<WireGuardVpnTunnel> configList = app.getSettings().getTunnels();

        for (int x = 0; x < configList.size(); x++) {
            WireGuardVpnTunnel tunnel = configList.get(x);
            if (tunnel.getPublicKey().equals(finder)) return (tunnel);
        }

        return (null);
    }
}
