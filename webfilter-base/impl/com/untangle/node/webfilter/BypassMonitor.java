/*
 * $HeadURL: svn://chef/work/src/webfilter-base/impl/com/untangle/node/webfilter/BypassMonitor.java $
 * Copyright (c) 2003-2009 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.webfilter;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.util.Worker;
import com.untangle.uvm.util.WorkerRunner;

/**
 * Regularly monitor user-bypassed sites and expire them after a
 * certain delay.
 *
 * @author <a href="mailto:seb@untangle.com">Sébastien Delafond</a>
 * @version 1.0
 */
class BypassMonitor {

    // static variables ---------------------------------------------------------
    private static long BYPASS_TIMEOUT_MS = 60l * 60l * 1000l;
    private static long BYPASS_SLEEP_DELAY_MS = 20l * 60l * 1000l;
    static {
        if (System.getProperty("com.untangle.node.webfilter.bypass-timeout") != null)
            BYPASS_TIMEOUT_MS = Long.parseLong(System.getProperty("com.untangle.node.webfilter.bypass-timeout"));

        if (System.getProperty("com.untangle.node.webfilter.bypass-sleep-delay") != null)
            BYPASS_SLEEP_DELAY_MS = Long.parseLong(System.getProperty("com.untangle.node.webfilter.bypass-sleep-delay"));
    }

    // private members ---------------------------------------------------------
    private final WebFilterBase wfb;
    private final Logger logger = Logger.getLogger(getClass());
    private final Monitor monitor = new Monitor();
    private final WorkerRunner workerRunner = new WorkerRunner(monitor, LocalUvmContextFactory.context());

    // constructors -----------------------------------------------
    public BypassMonitor(WebFilterBase myWfb) {
        logger.info("BypassMonitor initializing");
        wfb = myWfb;
    }

    // class protected methods -----------------------------------------------
    void addBypassedSite(InetAddress addr, String site) {
        monitor.addBypassedSite(addr, site);
    }

    void start() {
        workerRunner.start();
    }

    void stop()  {
        workerRunner.stop();
    }

    // inner classes ---------------------------------------------------------
    /**
     * The Monitor class actually takes care of periodically checking
     * host-bypassed sites, and removing them if they are expired.
     */
    private final class Monitor implements Worker {
        private SortedSet<BypassedSite> bypassedSites = new TreeSet();
	
        public synchronized void addBypassedSite(InetAddress addr, String site) {
            BypassedSite bs = new BypassedSite(addr, site);
            if (bypassedSites.contains(bs))
                bypassedSites.remove(bs);  // to make sure creation time is the latest...
            bypassedSites.add(bs);

            logger.warn("added " + bs);
        }

        public void start() { // nothing special to run
        }

        public void stop() { // nothing special to run
        }

        public void work() throws InterruptedException {
            Thread.sleep(BYPASS_SLEEP_DELAY_MS);

            if (bypassedSites.isEmpty())
                return;

            long expirationTime = System.nanoTime() / 1000000l - BYPASS_TIMEOUT_MS;
            if (bypassedSites.first().creationTimeMillis > expirationTime)
                return;

            try {
                Map<InetAddress,List<String>> sitesToDelete = new HashMap<InetAddress,List<String>>();
                synchronized(this) {
                    Iterator iter = bypassedSites.iterator();
                    BypassedSite bs;
                    List<String> l;

                    while (iter.hasNext()) {
                        bs = (BypassedSite)iter.next();
                        logger.warn("looking at " + bs);

                        if (bs.creationTimeMillis > expirationTime) {
                            logger.warn(".. not expired yet");
                            break; // ordered, so we can stop right here
                        }

                        logger.warn(".. expired, scheduling for removal");
                        iter.remove();

                        // add to sitesToDelete
                        if (sitesToDelete.containsKey(bs.addr))
                            sitesToDelete.get(bs.addr).add(bs.site);
                        else {
                            l = new ArrayList<String>();
                            l.add(bs.site);
                            sitesToDelete.put(bs.addr, l);
                        }
                    }
                }
                wfb.getBlacklist().removeUnblockedSites(sitesToDelete);
            } catch (Exception e) {
                logger.warn("Problem in BypassMonitor: '" + e.getMessage() + "'", e);
            }
        }
    }

    /**
     * A site bypassed by a specific host. Note that this class has a
     * natural ordering that is inconsistent with equals(): we
     * consider two BypassedSites to be equal if the host and the URL
     * are equals, but we order them by creation time.
     */
    private final class BypassedSite implements Comparable {
        private String site;
        private InetAddress addr;
        private long creationTimeMillis;
	
        public BypassedSite(InetAddress myAddr, String mySite) {
            site = mySite;
            addr = myAddr;
            creationTimeMillis = System.nanoTime() / 1000000L;
        }

        public boolean equals(BypassedSite other) {
            return ((site == other.site) && (addr == other.addr));
        }

        public int hashCode() {
            return (17 + 37 * (site.hashCode() + addr.hashCode()));
        }
	
        public int compareTo(Object other) {
            return (int)(creationTimeMillis - ((BypassedSite)other).creationTimeMillis);
        }
	
        public String toString() {
            return "Host-bypassed site {" + addr + ", " + site + "}";
        }

    }
}