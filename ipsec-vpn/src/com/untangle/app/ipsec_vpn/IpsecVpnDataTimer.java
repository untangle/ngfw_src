/**
 * $Id: IpsecVpnDataTimer.java 37267 2014-02-26 23:42:19Z dmorris $
 */

package com.untangle.app.ipsec_vpn;

import java.util.LinkedList;
import java.util.TimerTask;
import java.util.Hashtable;
import java.util.Iterator;
import java.net.InetAddress;

import org.apache.log4j.Logger;

/**
 * This is our DataTimer class that runs periodically to capture and record
 * tunnel traffic statistics for reporting purposes.
 * 
 * @author mahotz
 * 
 */

public class IpsecVpnDataTimer extends TimerTask
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
        long cycleMark;
        long outLast;
        long inLast;

        /**
         * Constructor
         * 
         * @param tunnelName
         *        The name of the tunnel
         */
        public TunnelWatcher(String tunnelName)
        {
            this.tunnelName = tunnelName;
            cycleMark = 0;
            outLast = 0;
            inLast = 0;
        }
    }

    private final String TUNNEL_STATUS_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-tunnel-status";
    private final Logger logger = Logger.getLogger(getClass());
    private final IpsecVpnApp app;

    private Hashtable<String, TunnelWatcher> watchTable = new Hashtable<>();
    private long cycleCounter = 0;

    /**
     * Constructor
     * 
     * @param app
     *        The application instance that created us
     */
    public IpsecVpnDataTimer(IpsecVpnApp app)
    {
        this.app = app;
    }

    /**
     * This is the timer run function
     */
    public void run()
    {
        ProcessAllTunnels();
    }

    /**
     * Function to process all tunnels
     */
    private void ProcessAllTunnels()
    {
        LinkedList<ConnectionStatusRecord> statusList = app.getTunnelStatus();
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
                logger.debug("Creating new data table entry for " + status.getWorkName());
                watcher = new TunnelWatcher(status.getWorkName());
                watchTable.put(status.getWorkName(), watcher);
            }

            else {
                logger.debug("Found existing data table entry for " + status.getWorkName());
            }

            // update the cycle mark for all watcher objects that match an enabled IPsec
            // tunnel.  we'll use this later to remove any stale watcher entries
            watcher.cycleMark = cycleCounter;

            // if the tunnel is active grab the traffic stats
            if (status.getMode().toLowerCase().equals("active")) {
                GrabTunnelStatistics(watcher);
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
                logger.debug("Removing stale data table entry for " + watcher.tunnelName);
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
}
