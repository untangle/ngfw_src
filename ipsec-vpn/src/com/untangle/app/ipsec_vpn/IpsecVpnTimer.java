/**
 * $Id: IpsecVpnTimer.java 37267 2014-02-26 23:42:19Z dmorris $
 */

package com.untangle.app.ipsec_vpn;

import java.util.LinkedList;
import java.util.TimerTask;
import java.util.Hashtable;
import java.util.Iterator;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ExecManagerResult;

import org.apache.log4j.Logger;

/*
 * Sometimes we see tunnels that have been connected for days suddenly just go
 * away, almost like ipsec just forgot about them.  The only solution we've come
 * up with is this awful code that periodically looks at the status of all
 * tunnels, and uses the ipsec down/up utility to force the tunnel to reconnect.
 *
 * Since we're already monitoring every minute, we're also using this code
 * to capture and record traffic statistics for reporting purposes.
 */

public class IpsecVpnTimer extends TimerTask
{
    class TunnelWatcher
    {
        String tunnelName;
        long activeCounter;
        long cycleMark;
        long outLast;
        long inLast;

        public TunnelWatcher(String tunnelName)
        {
            this.tunnelName = tunnelName;
            activeCounter = 0;
            cycleMark = 0;
            outLast = 0;
            inLast = 0;
        }
    }

    private final String TUNNEL_STATS_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-tunnel-stats";
    private final Logger logger = Logger.getLogger(getClass());
    private final IpsecVpnApp app;

    // the mininum number of minutes (assuming 600000 msec timer calling interval) for
    // a tunnel to be detected active before we try to restart if detected down
    private final long RESTART_THRESHOLD = 5;

    private Hashtable<String, TunnelWatcher> watchTable = new Hashtable<String, TunnelWatcher>();
    private long cycleCounter = 0;

    public IpsecVpnTimer(IpsecVpnApp app)
    {
        this.app = app;
    }

    public void run()
    {
        CheckAllTunnels();
    }

    public void CheckAllTunnels()
    {
        LinkedList<ConnectionStatusRecord> statusList = app.getTunnelStatus();
        TunnelWatcher watcher;
        cycleCounter += 1;
        String workname;
        String key;
        int x;

        // start by adding or updating watchTable with all enabled IPsec tunnels
        for (x = 0; x < statusList.size(); x++) {
            ConnectionStatusRecord status = statusList.get(x);

            // Use the id and description to create a unique connection name that won't cause
            // problems in the ipsec.conf file by replacing non-word characters with a hyphen.
            // We also prefix this name with UT123_ to ensure no dupes in the config file.
            workname = ("UT" + status.getId() + "_" + status.getDescription().replaceAll("\\W", "-"));

            // see if there is an existing entry in the watch table
            watcher = watchTable.get(workname);

            if (watcher == null) {
                logger.debug("Creating new watch table entry for " + workname);
                watcher = new TunnelWatcher(workname);
                watchTable.put(workname, watcher);
            }

            else {
                logger.debug("Found existing watch table entry for " + workname);
            }

            // update the cycle mark for all watcher objects that match an enabled IPsec
            // tunnel.  we'll use this later to remove any stale watcher entries
            watcher.cycleMark = cycleCounter;

            // if the tunnel is active increment the active counter and grab the traffic stats
            if (status.getMode().toLowerCase().equals("active")) {
                watcher.activeCounter += 1;
                GrabTunnelStatistics(watcher);
            }

            // ipsec screwed up again... or rather, the tunnel appears to be down
            else {

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

    void GrabTunnelStatistics(TunnelWatcher watcher)
    {
        long outValue = 0;
        long inValue = 0;
        long outBytes = 0;
        long inBytes = 0;
        int top, len;
        String result;

        /*
         * the script should return the tunnel statis in the following format...
         * | TUNNNEL:tunnel_name IN:123 OUT:456 |
         */

        result = IpsecVpnApp.execManager().execOutput(TUNNEL_STATS_SCRIPT + " " + watcher.tunnelName);

        /*
         * We use the IN: and OUT: tags to find the beginning of each value and
         * the trailing space to isolate the numeric portions of the string to
         * keep Long.valueOf happy.
         */

        try {
            top = result.indexOf("IN:");
            if (top > 0) {
                len = result.substring(top + 3).indexOf(" ");
                if (len > 0) {
                    inValue = Long.valueOf(result.substring(top + 3, top + 3 + len));
                }
            }

            top = result.indexOf("OUT:");
            if (top > 0) {
                len = result.substring(top + 4).indexOf(" ");
                if (len > 0) {
                    outValue = Long.valueOf(result.substring(top + 4, top + 4 + len));
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

        if (inValue < watcher.inLast)
            inBytes = inValue;
        else
            inBytes = (inValue - watcher.inLast);

        if (outValue < watcher.outLast)
            outBytes = outValue;
        else
            outBytes = (outValue - watcher.outLast);

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
