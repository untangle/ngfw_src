/**
 * $Id: IpsecVpnPingTimer.java 37267 2014-02-26 23:42:19Z dmorris $
 */

package com.untangle.app.ipsec_vpn;

import java.util.LinkedList;
import java.util.TimerTask;
import java.util.Hashtable;
import java.util.Iterator;
import java.net.InetAddress;

import org.apache.log4j.Logger;

/**
 * The PingTimer class runs periodically and uses ping to determine if tunnels
 * are active or have stopped responding. It generates up and down events and
 * will attempt to restart tunnels that seem to have failed.
 * 
 * @author mahotz
 * 
 */

public class IpsecVpnPingTimer extends TimerTask
{

    /**
     * Define an object we can use to keep track of each IPsec tunnel.
     * 
     * @author mahotz
     * 
     */
    class TunnelWatcher
    {
        String tunnelName;
        String controlName;
        int tunnelId;
        boolean activeFlag;
        long cycleMark;
        long failCounter;

        /**
         * Constructor
         * 
         * @param tunnelName
         *        The name of the tunnel
         *
         * @param tunnelId
         *        The ID of the tunnel
         */
        public TunnelWatcher(String tunnelName, int tunnelId)
        {
            this.tunnelName = tunnelName;
            this.tunnelId = tunnelId;
            activeFlag = false;
            failCounter = 0;
            cycleMark = 0;
            controlName = ("UT" + Integer.toString(tunnelId) + "_" + tunnelName);
        }
    }

    private final Logger logger = Logger.getLogger(getClass());
    private final IpsecVpnApp app;

    // the number of ping failures before we force a tunnel restart
    private final long PING_FAIL_THRESHOLD = 3;

    private Hashtable<String, TunnelWatcher> watchTable = new Hashtable<>();
    private long cycleCounter = 0;

    /**
     * Constructor
     * 
     * @param app
     *        The application instance that created us
     */
    public IpsecVpnPingTimer(IpsecVpnApp app)
    {
        this.app = app;
    }

    /**
     * This is the timer run function
     */
    public void run()
    {
        CheckAllTunnels();
    }

    /**
     * Function to check all IPsec tunnels with a configured ping target and
     * restart any that seem to be down.
     */
    private void CheckAllTunnels()
    {
        LinkedList<IpsecVpnTunnel> configList = app.getSettings().getTunnels();
        IpsecVpnTunnel tunnel;
        TunnelWatcher watcher;
        cycleCounter += 1;
        String key;
        int x;

        // process the list of configured tunnels
        for (x = 0; x < configList.size(); x++) {
            tunnel = configList.get(x);

            // ignore disabled tunnels and those with no ping target configured
            if (tunnel.getActive() == false) continue;
            if (tunnel.getPingAddress() == null) continue;
            if (tunnel.getPingAddress().length() == 0) continue;

            // see if there is an existing entry in the watch table
            watcher = watchTable.get(tunnel.getDescription());

            if (watcher == null) {
                logger.debug("Creating new ping table entry for " + tunnel.getDescription());
                watcher = new TunnelWatcher(tunnel.getDescription(), tunnel.getId());
                watchTable.put(tunnel.getDescription(), watcher);
            }

            else {
                logger.debug("Found existing ping table entry for " + tunnel.getDescription());
            }

            // update the cycle mark for the current tunnel
            watcher.cycleMark = cycleCounter;

            // check the ping target
            boolean active = CheckTunnelPingTarget(tunnel);

            // if the tunnel is active increment the active counter
            if (active == true) {
                // if the active flag is false we transition from down to up 
                if (watcher.activeFlag == false) {
                    watcher.activeFlag = true;
                    watcher.failCounter = 0;
                    IpsecVpnEvent event = new IpsecVpnEvent(tunnel.getLeft(), tunnel.getRight(), tunnel.getDescription(), IpsecVpnEvent.EventType.CONNECT);
                    app.logEvent(event);
                    logger.debug("logEvent(ipsec_vpn_events) " + event.toSummaryString());
                }

                // continue the tunnel loop
                continue;
            }

            /*
             * At this point the tunnel is enabled but the ping failed. We check
             * the restart interval, and do event logging and force restart as
             * required.
             */

            // if our active flag is true we need to log a down event
            if (watcher.activeFlag == true) {
                watcher.activeFlag = false;
                IpsecVpnEvent event = new IpsecVpnEvent(tunnel.getLeft(), tunnel.getRight(), tunnel.getDescription(), IpsecVpnEvent.EventType.DISCONNECT);
                app.logEvent(event);
                logger.debug("logEvent(ipsec_vpn_events) " + event.toSummaryString());
            }

            // increment the ping fail counter for the tunnel
            watcher.failCounter++;

            // if we haven't reached the fail threshold just continue
            if (watcher.failCounter < PING_FAIL_THRESHOLD) continue;

            // fail threshold reached so bring tunnel down and back up
            logger.warn("Attempting restart for inactive tunnel " + watcher.controlName);
            IpsecVpnApp.execManager().exec("ipsec down " + watcher.controlName);

            // run the up command in the background as it can block if the other side is unreachable
            IpsecVpnApp.execManager().exec("nohup ipsec up " + watcher.controlName + " >/dev/null 2>&1 &");
        }

        Iterator<String> ksi = watchTable.keySet().iterator();

        while (ksi.hasNext()) {
            key = ksi.next();
            watcher = watchTable.get(key);

            // if the cycle mark on this object doesn't match the current cycle counter
            // it means we didn't find an enabled IPsec tunnel so we remove the entry
            // using the iterator remove function since the Java docs say it's safe
            if (watcher.cycleMark != cycleCounter) {
                logger.debug("Removing stale ping table entry for " + watcher.tunnelName);
                ksi.remove();
            }
        }
    }

    /**
     * Function to ping a remote host across an IPsec tunnel
     * 
     * @param tunnel
     *        The IpsecVpnTunnel for the ping test
     *
     * @return true for ping success, false for fail
     */
    private boolean CheckTunnelPingTarget(IpsecVpnTunnel tunnel)
    {
        if (tunnel == null) return (false);
        if (tunnel.getPingAddress() == null) return (false);
        if (tunnel.getPingAddress().length() == 0) return (false);

        try {
            InetAddress target = InetAddress.getByName(tunnel.getPingAddress());
            if (target.isReachable(2000)) {
                logger.debug("PING SUCCESS: " + tunnel.getPingAddress());
                return (true);
            }
        } catch (Exception exn) {
            logger.debug("PING EXCEPTION: " + tunnel.getPingAddress(), exn);
        }

        IpsecVpnEvent event = new IpsecVpnEvent(tunnel.getLeft(), tunnel.getRight(), tunnel.getDescription(), IpsecVpnEvent.EventType.UNREACHABLE);
        app.logEvent(event);
        logger.debug("logEvent(ipsec_vpn_events) " + event.toSummaryString());
        return (false);
    }
}
