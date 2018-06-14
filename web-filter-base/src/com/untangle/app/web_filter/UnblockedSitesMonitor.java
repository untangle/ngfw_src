/**
 * $Id$
 */

package com.untangle.app.web_filter;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;

/**
 * Regularly monitor user-unblocked sites and expire them after a certain delay.
 * 
 */
public class UnblockedSitesMonitor
{
    private static long MONITOR_SLEEP_DELAY_MS = 5l * 60l * 1000l;

    private final WebFilterBase wfb;
    private final Logger logger = Logger.getLogger(getClass());
    private final Monitor monitor = new Monitor();

    /**
     * Constructor
     * 
     * @param myWfb
     *        The base web filter app
     */
    public UnblockedSitesMonitor(WebFilterBase myWfb)
    {
        logger.info("UnblockedSitesMonitor initializing");
        wfb = myWfb;
    }

    /**
     * Add an unblocked site
     * 
     * @param addr
     *        The address
     * @param site
     *        The site
     */
    void addUnblockedSite(InetAddress addr, String site)
    {
        monitor.addUnblockedSite(addr, site);
    }

    /**
     * Start the monitor
     */
    void start()
    {
        this.monitor.running = true;
        UvmContextFactory.context().newThread(this.monitor).start();
    }

    /**
     * Stop the monitor
     */
    void stop()
    {
        this.monitor.running = false;
    }

    /**
     * The Monitor class actually takes care of periodically checking
     * host-unblocked sites, and removing them if they are expired.
     */
    private final class Monitor implements Runnable
    {
        protected boolean running = true;

        private SortedSet<UnblockedSite> unblockedSites = new TreeSet<UnblockedSite>();

        /**
         * Remove all sites
         */
        public synchronized void flushAll()
        {
            this.unblockedSites = new TreeSet<UnblockedSite>();
        }

        /**
         * Add an unblocked site
         * 
         * @param addr
         *        The address
         * @param site
         *        The site
         */
        public synchronized void addUnblockedSite(InetAddress addr, String site)
        {
            UnblockedSite bs = new UnblockedSite(addr, site);
            if (unblockedSites.contains(bs)) unblockedSites.remove(bs); // to make sure creation time is the latest...
            unblockedSites.add(bs);

            logger.info("Adding unblock:" + bs);
        }

        /**
         * Thread run function
         */
        public void run()
        {
            while (true) {

                try {
                    Thread.sleep(MONITOR_SLEEP_DELAY_MS);
                } catch (Exception e) {
                }

                if (!running) return;

                if (unblockedSites.isEmpty()) continue;

                long expirationTime = System.currentTimeMillis() - (wfb.getSettings().getUnblockTimeout() * 1000);

                /**
                 * If the first site in the list is not yet expired and the list
                 * is order There is no need to traverse the whole list
                 */
                if (unblockedSites.first().creationTimeMillis > expirationTime) continue;

                try {
                    Map<InetAddress, List<String>> sitesToDelete = new HashMap<InetAddress, List<String>>();
                    synchronized (this) {
                        Iterator<UnblockedSite> iter = unblockedSites.iterator();
                        UnblockedSite bs;
                        List<String> l;

                        while (iter.hasNext()) {
                            bs = iter.next();
                            logger.info("Evaluating unblock \"" + bs + "\"");

                            if (bs.creationTimeMillis > expirationTime) {
                                logger.info("Evaluating unblock \"" + bs + "\": still valid");
                                break; // ordered, so we can stop right here
                            }

                            logger.info("Evaluating unblock \"" + bs + "\": expired (" + bs.creationTimeMillis + " > " + expirationTime + ")");
                            iter.remove();

                            // add to sitesToDelete
                            if (sitesToDelete.containsKey(bs.addr)) sitesToDelete.get(bs.addr).add(bs.site);
                            else {
                                l = new ArrayList<String>();
                                l.add(bs.site);
                                sitesToDelete.put(bs.addr, l);
                            }
                        }
                    }
                    wfb.getDecisionEngine().removeUnblockedSites(sitesToDelete);
                } catch (Exception e) {
                    logger.warn("Problem in UnblockedSitesMonitor: '" + e.getMessage() + "'", e);
                }
            }
        }
    }

    /**
     * A site unblocked by a specific host. Note that this class has a natural
     * ordering that is inconsistent with equals(): we consider two
     * UnblockedSites to be equal if the host and the URL are equals, but we
     * order them by creation time.
     */
    private final class UnblockedSite implements Comparable<UnblockedSite>
    {
        private String site;
        private InetAddress addr;
        private long creationTimeMillis;

        /**
         * Constructor
         * 
         * @param myAddr
         *        The address
         * @param mySite
         *        The site
         */
        public UnblockedSite(InetAddress myAddr, String mySite)
        {
            site = mySite;
            addr = myAddr;
            creationTimeMillis = System.currentTimeMillis();
        }

        /**
         * Generate a hash code from the site and address
         * 
         * @return The hashcode
         */
        public int hashCode()
        {
            return (17 + 37 * (site.hashCode() + addr.hashCode()));
        }

        /**
         * Compare with another UnblockedSite object
         * 
         * @param other
         *        The object for comparision
         * @return Comparison result
         */
        public int compareTo(UnblockedSite other)
        {
            return (int) (creationTimeMillis - other.creationTimeMillis);
        }

        /**
         * Create string representation
         * 
         * @return The string representation
         */
        public String toString()
        {
            return "Unblock {" + addr + ", " + site + "}";
        }
    }
}
