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
        String faceName;
        String publicKey;

        long faceTraffic;
        boolean faceAlive;

        long lastFaceRx;
        long lastFaceTx;
        long currFaceRx;
        long currFaceTx;

        long lastWgRx;
        long lastWgTx;

        long lastPing;
        long lastUpdate;

        /**
         * Constructor
         * 
         * @param faceName
         *        The network interface of the tunnel
         * @param publicKey
         *        The public key of the tunnel
         */
        public TunnelWatcher(String faceName, String publicKey)
        {
            this.faceName = faceName;
            this.publicKey = publicKey;

            faceTraffic = 0;
            faceAlive = false;

            lastFaceRx = 0;
            lastFaceTx = 0;
            currFaceRx = 0;
            currFaceTx = 0;

            lastWgRx = 0;
            lastWgTx = 0;

            lastPing = 0;
            lastUpdate = 0;
        }
    }

    /* Pattern for parsing interface details from /proc/net/dev */
    private static final Pattern NET_DEV_PATTERN = Pattern.compile("^\\s*([a-z0-9\\.]+\\d+):\\s*(\\d+)\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+(\\d+)");

    private static final long THREAD_JOIN_TIME_MSEC = 1000; // milliseconds delay to allow thread join when shutting down
    private static final long TUNNEL_ACTIVITY_TIMEOUT = 60; // seconds to wait before considering a tunnel down

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
                Thread.sleep(5000);
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
        BufferedReader reader = null;
        JSONTokener jsonTokener;
        JSONObject jsonObject;
        JSONArray statusList;

        /*
         * Start by getting the raw statistics for all interfaces
         */
        try {
            reader = new BufferedReader(new FileReader("/proc/net/dev"));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                // use the matcher to extract device(1), rx(2), tx(3)
                Matcher matcher = NET_DEV_PATTERN.matcher(line);
                if (!matcher.find()) continue;

                // find the watcher for the device and update the current interface byte counts
                TunnelWatcher watcher = watchTable.get(matcher.group(1));
                if (watcher == null) continue;
                watcher.currFaceRx = Long.parseLong(matcher.group(2));
                watcher.currFaceTx = Long.parseLong(matcher.group(3));
            }

        } catch (Exception exn) {
            logger.warn("Exception parsing /proc/net/dev:", exn);
        }

        if (reader != null) {
            try {
                reader.close();
            } catch (Exception exn) {
            }
        }

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
            String facename;
            String peerkey;

            try {
                // get the tunnel status object at the current index
                status = statusList.getJSONObject(x);
            } catch (Exception exn) {
                logger.warn("Error creating status array:", exn);
                continue;
            }

            try {
                // get the interface from the status record
                facename = status.getString("interface");
            } catch (Exception exn) {
                logger.warn("Error getting interface:", exn);
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
            TunnelWatcher watcher = watchTable.get(facename);

            if (watcher == null) {
                logger.debug("Creating new watch table entry for " + facename);
                watcher = new TunnelWatcher(facename, peerkey);
                watchTable.put(facename, watcher);
            } else {
                logger.debug("Found existing watch table entry for " + facename);
            }

            generateTunnelStatistics(tunnel, watcher, status);
            checkTunnelConnection(tunnel, watcher);
            handleTunnelPingCheck(tunnel, watcher);
        }

        Set<String> keylist = watchTable.keySet();
        long nowtime = Instant.now().getEpochSecond();

        // look for stale entries in the watch table
        for (String key : keylist) {
            TunnelWatcher watcher = watchTable.get(key);

            // continue if the entry has been updated in the last hour
            if (nowtime < (watcher.lastUpdate + 3600)) continue;

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

        /*
         * If this is the first time checking this tunnel we need to use the
         * current RX and TX values as the starting point for delta caculations.
         */
        if (watcher.lastUpdate == 0) {
            watcher.lastUpdate = Instant.now().getEpochSecond();
            watcher.lastWgRx = inValue;
            watcher.lastWgTx = outValue;
            return;
        }

        // set the lastUpdate which is used by the table cleanup logic
        watcher.lastUpdate = Instant.now().getEpochSecond();

        // if neither value changed there is nothing to log so just return
        if ((inValue == watcher.lastWgRx) && (outValue == watcher.lastWgTx)) return;

        /*
         * We calculate the rx and tx bytes since the last time we checked. If
         * the current values are less than we saw during the previous check we
         * assume the tunnel or counters were somehow reset and just use the new
         * the values as the raw byte counts for this iteration.
         */

        if (inValue < watcher.lastWgRx) inBytes = inValue;
        else inBytes = (inValue - watcher.lastWgRx);

        if (outValue < watcher.lastWgTx) outBytes = outValue;
        else outBytes = (outValue - watcher.lastWgTx);

        watcher.lastWgRx = inValue;
        watcher.lastWgTx = outValue;

        WireguardVpnStats event = new WireguardVpnStats(tunnel.getDescription(), tunnel.getPeerAddress(), inBytes, outBytes);
        app.logEvent(event);
        logger.debug("GrabTunnelStatistics(logEvent) " + event.toString());
    }

    /**
     * Function to determine if a tunnel is actually connected
     *
     * @param tunnel
     *        - The tunnel object
     * @param watcher
     *        - The watcher object
     */
    private void checkTunnelConnection(WireguardVpnTunnel tunnel, TunnelWatcher watcher)
    {
        long nowtime = Instant.now().getEpochSecond();

        // if current interface counts are less than previous they overflowed or were reset 
        if (watcher.currFaceRx < watcher.lastFaceRx) watcher.lastFaceRx = 0;
        if (watcher.currFaceTx < watcher.lastFaceTx) watcher.lastFaceTx = 0;

        // if the counters have not changed we may need to handle state transition  
        if ((watcher.currFaceRx == watcher.lastFaceRx) && (watcher.currFaceTx == watcher.lastFaceTx)) {
            // if we have not reached the timeout just return
            if (nowtime < (watcher.faceTraffic + TUNNEL_ACTIVITY_TIMEOUT)) return;

            // if the alive flag is clear there is nothing to do
            if (watcher.faceAlive == false) return;

            // clear the alive flag
            watcher.faceAlive = false;

            // log the DISCONNECT event
            WireguardVpnEvent event = new WireguardVpnEvent(tunnel.getDescription(), WireguardVpnEvent.EventType.DISCONNECT);
            app.logEvent(event);
            logger.debug("logEvent(wireguard_vpn_events) " + event.toSummaryString());

            return;
        }

        // the couters have changed so update them and the last traffic time
        watcher.faceTraffic = nowtime;
        watcher.lastFaceRx = watcher.currFaceRx;
        watcher.lastFaceTx = watcher.currFaceTx;

        // if the alive flag is set there is nothing to do
        if (watcher.faceAlive == true) return;

        // clear the alive flag
        watcher.faceAlive = true;

        // log the CONNECT event
        WireguardVpnEvent event = new WireguardVpnEvent(tunnel.getDescription(), WireguardVpnEvent.EventType.CONNECT);
        app.logEvent(event);
        logger.debug("logEvent(wireguard_vpn_events) " + event.toSummaryString());
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

        // check the tunnel ping interval and return if below
        long nowtime = Instant.now().getEpochSecond();
        if (nowtime < (watcher.lastPing + tunnel.getPingInterval())) return;

        // update the last ping time
        watcher.lastPing = nowtime;

        try {
            InetAddress target = InetAddress.getByName(tunnel.getPingAddress().getHostAddress());
            if (target.isReachable(2000)) {
                logger.debug("PING SUCCESS: " + tunnel.getPingAddress().getHostAddress());
                return;
            }
        } catch (Exception exn) {
            logger.debug("PING EXCEPTION: " + tunnel.getPingAddress().getHostAddress(), exn);
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
