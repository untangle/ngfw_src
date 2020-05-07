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
 * This is our timer class where we handle periodic tasks. Sometimes we see
 * tunnels that have been connected for days suddenly just go away, almost like
 * ipsec just forgot about them. The only solution we've come up with is this
 * awful code that periodically looks at the status of all tunnels, and uses the
 * ipsec down/up utility to force the tunnel to reconnect.
 * 
 * Since we're already monitoring every minute, we're also using this code to
 * capture and record traffic statistics for reporting purposes.
 * 
 * We also added the ability to ping a host across the tunnel, so we use the
 * timer to implement that functionality as well.
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
        int tunnelId;
        String tunnelName;
        boolean activeFlag;
        long activeCounter;
        long cycleMark;
        long outLast;
        long inLast;
        int pingCounter;

        /**
         * Constructor
         * 
         * @param tunnelName
         *        The name of the tunnel
         * @param tunnelId
         *        The ID of the tunnel
         */
        public TunnelWatcher(String tunnelName, int tunnelId)
        {
            this.tunnelName = tunnelName;
            this.tunnelId = tunnelId;
            activeFlag = false;
            activeCounter = 0;
            cycleMark = 0;
            outLast = 0;
            inLast = 0;
            pingCounter = 0;
        }
    }

    private final String TUNNEL_STATUS_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-tunnel-status";
    private final Logger logger = Logger.getLogger(getClass());
    private final IpsecVpnApp app;

    // the mininum number of minutes (assuming 60000 msec timer calling interval) for
    // a tunnel to be detected active before we try to restart if detected down
    private final long RESTART_THRESHOLD = 5;

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
     * Function to check the status of all IPsec tunnels, and restart any that
     * seem to be down.
     */
    private void CheckAllTunnels()
    {
        LinkedList<ConnectionStatusRecord> statusList = app.getTunnelStatus();
        IpsecVpnTunnel tunnel;
        TunnelWatcher watcher;
        cycleCounter += 1;
        String key;
        int x;

        // start by adding or updating watchTable with all enabled IPsec tunnels
        for (x = 0; x < statusList.size(); x++) {
            ConnectionStatusRecord status = statusList.get(x);

            // see if there is an existing entry in the watch table
            watcher = watchTable.get(status.getWorkName());

            if (watcher == null) {
                logger.debug("Creating new watch table entry for " + status.getWorkName());
                watcher = new TunnelWatcher(status.getWorkName(), Integer.parseInt(status.getId()));
                watchTable.put(status.getWorkName(), watcher);
            }

            else {
                logger.debug("Found existing watch table entry for " + status.getWorkName());
            }

            tunnel = findTunnelById(watcher.tunnelId);

            // update the cycle mark for all watcher objects that match an enabled IPsec
            // tunnel.  we'll use this later to remove any stale watcher entries
            watcher.cycleMark = cycleCounter;

            // if the tunnel is active increment the active counter and grab the traffic stats
            if (status.getMode().toLowerCase().equals("active")) {
                watcher.activeCounter += 1;
                GrabTunnelStatistics(watcher);

                if (tunnel != null) {
                    CheckTunnelPingTarget(watcher, tunnel);

                    // if the active flag is false we need to log an up event 
                    if (watcher.activeFlag == false) {
                        watcher.activeFlag = true;
                        IpsecVpnEvent event = new IpsecVpnEvent(tunnel.getLeft(), tunnel.getRight(), tunnel.getDescription(), IpsecVpnEvent.EventType.CONNECT);
                        app.logEvent(event);
                        logger.debug("logEvent(ipsec_vpn_events) " + event.toSummaryString());
                    }
                }
            }

            // ipsec screwed up again... or rather, the tunnel appears to be down
            else {

                // if our active flag is true we need to log a down event
                if (watcher.activeFlag == true && tunnel != null) {
                    watcher.activeFlag = false;
                    IpsecVpnEvent event = new IpsecVpnEvent(tunnel.getLeft(), tunnel.getRight(), tunnel.getDescription(), IpsecVpnEvent.EventType.DISCONNECT);
                    app.logEvent(event);
                    logger.debug("logEvent(ipsec_vpn_events) " + event.toSummaryString());
                }

                // if the tunnel was detected as active for the minimum amount of time
                // then we will attempt restart by calling ipsec down and ipsec up
                if (watcher.activeCounter > RESTART_THRESHOLD) {
                    long hval = (watcher.activeCounter / 60);
                    long mval = (watcher.activeCounter % 60);
                    logger.warn("Attempting restart for inactive tunnel " + watcher.tunnelName + " (UPTIME = " + Long.toString(hval) + " hours " + Long.toString(mval) + " minutes)");
                    IpsecVpnApp.execManager().exec("ipsec down " + watcher.tunnelName);

                    // run the up command in the background as it can block if the other side is unreachable
                    IpsecVpnApp.execManager().exec("nohup ipsec up " + watcher.tunnelName + " >/dev/null 2>&1 &");
                } else {
                    logger.debug("Ignoring inactive tunnel " + watcher.tunnelName);
                }

                // finally we reset the active counter so no further attempt will be made
                // to restart the tunnel until it has once again been detected as active
                // for the minimum amount of time.
                watcher.activeCounter = 0;
            }
        }

        Iterator<String> ksi = watchTable.keySet().iterator();

        while (ksi.hasNext()) {
            key = ksi.next();
            watcher = watchTable.get(key);

            // if the cycle mark on this object doesn't match the cycle counter it
            // means we didn't find an enabled IPsec tunnel so we remove the entry
            // using the iterator remove function since the Java docs say it's safe
            if (watcher.cycleMark != cycleCounter) {
                logger.debug("Removing stale watchTable entry for " + watcher.tunnelName);
                ksi.remove();
            }
        }
    }

    /**
     * Function to capture and record the traffic statistics for each active
     * tunnel.
     * 
     * @param watcher
     *        The TunnelWatcher for the tunnel to be checked
     */
    private void GrabTunnelStatistics(TunnelWatcher watcher)
    {
        long outValue = 0;
        long inValue = 0;
        long outBytes = 0;
        long inBytes = 0;
        int top, wid, len;
        String result;

// THIS IS FOR ECLIPSE - @formatter:off

        /*
         * the script should return the tunnel status in the following format:
         * | TUNNNEL:tunnel_name LOCAL:1.2.3.4 REMOTE:5.6.7.8 STATE:active IN:123 OUT:456 |
         */
        result = IpsecVpnApp.execManager().execOutput(TUNNEL_STATUS_SCRIPT + " " + watcher.tunnelName);

// THIS IS FOR ECLIPSE - @formatter:on

        /*
         * We use the IN: and OUT: tags to find the beginning of each value and
         * the trailing space to isolate the numeric portions of the string to
         * keep Long.valueOf happy.
         */

        try {
            top = result.indexOf("IN:");
            wid = 3;
            if (top > 0) {
                len = result.substring(top + wid).indexOf(" ");
                if (len > 0) {
                    inValue = Long.valueOf(result.substring(top + wid, top + wid + len));
                }
            }

            top = result.indexOf("OUT:");
            wid = 4;
            if (top > 0) {
                len = result.substring(top + wid).indexOf(" ");
                if (len > 0) {
                    outValue = Long.valueOf(result.substring(top + wid, top + wid + len));
                }
            }
        }

        /*
         * If we can't parse the tunnel stats just return
         */
        catch (Exception exn) {
            return;
        }

        /*
         * if neither value changed there is nothing to log so just return
         */
        if ((inValue == watcher.inLast) && (outValue == watcher.outLast)) return;

        /*
         * The stats for each tunnel seem to get cleared by the daemon each time
         * the tunnel is re-keyed, so we look for values less than we saw on the
         * previous check and handle accordingly. Once we calculate the number
         * of IN and OUT bytes since the last check, we update the last values
         * and write the stat record to the database.
         * 
         * The downside to this whole approach is that we won't count any
         * traffic on cycles with a re-key in between checks.
         * 
         * There is also an edge case where we could fail to record the first
         * traffic after a re-key if both the IN and OUT values are exactly the
         * same as they were during the last check.
         */

        if (inValue < watcher.inLast) inBytes = inValue;
        else inBytes = (inValue - watcher.inLast);

        if (outValue < watcher.outLast) outBytes = outValue;
        else outBytes = (outValue - watcher.outLast);

        watcher.inLast = inValue;
        watcher.outLast = outValue;

        // since we prepend some stuff to keep the tunnel names unique we
        // now look for it and strip it off to keep it out of the report
        String shortName = watcher.tunnelName;
        int marker = watcher.tunnelName.indexOf('_');
        if (marker > 0) shortName = watcher.tunnelName.substring(marker + 1);

        TunnelStatusEvent event = new TunnelStatusEvent(shortName, inBytes, outBytes);
        app.logEvent(event);
        logger.debug("GrabTunnelStatistics(logEvent) " + event.toString());
    }

    /**
     * Function to ping a remote host across a tunnel. The main goal here is to
     * give users a way to periodically generate traffic that goes across a
     * tunnel, and generate alerts if the ping target does not respond.
     * 
     * @param watcher
     *        The TunnelWatcher for the ping test
     * 
     * @param tunnel
     *        The IpsecVpnTunnel for the ping test
     */
    private void CheckTunnelPingTarget(TunnelWatcher watcher, IpsecVpnTunnel tunnel)
    {
        if (tunnel == null) return;
        if (tunnel.getPingInterval() == 0) return;
        if (tunnel.getPingAddress() == null) return;
        if (tunnel.getPingAddress().length() == 0) return;

        // no ping if we haven't reached the configured interval threshold
        watcher.pingCounter += 1;
        if (watcher.pingCounter < tunnel.getPingInterval()) return;

        try {
            InetAddress target = InetAddress.getByName(tunnel.getPingAddress());
            if (target.isReachable(2000)) {
                logger.debug("PING SUCCESS: " + tunnel.getPingAddress());
                return;
            }
        } catch (Exception exn) {
            logger.debug("PING EXCEPTION: " + tunnel.getPingAddress(), exn);
        }
        IpsecVpnEvent event = new IpsecVpnEvent(tunnel.getLeft(), tunnel.getRight(), tunnel.getDescription(), IpsecVpnEvent.EventType.UNREACHABLE);
        app.logEvent(event);
        logger.debug("logEvent(ipsec_vpn_events) " + event.toSummaryString());
    }

    /**
     * Function to find the IpsecVpnTunnel matching a given ID value.
     * 
     * @param idValue
     *        The ID value of the tunnel to find
     * 
     * @return The IpsecVpnTunnel if found, otherwise null
     */
    private IpsecVpnTunnel findTunnelById(int idValue)
    {
        LinkedList<IpsecVpnTunnel> configList = app.getSettings().getTunnels();

        // create a status display record for all enabled tunnels
        for (int x = 0; x < configList.size(); x++) {
            IpsecVpnTunnel tunnel = configList.get(x);
            if (tunnel.getId() == idValue) return (tunnel);
        }

        return (null);
    }
}
