/*
 * $HeadURL: svn://chef/work/src/webfilter-base/impl/com/untangle/node/webfilter/UnblockedSitesMonitor.java $
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

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.Worker;
import com.untangle.uvm.util.WorkerRunner;

/**
 * Regularly monitor user-unblocked sites and expire them after a
 * certain delay.
 *
 * @author <a href="mailto:seb@untangle.com">Sebastien Delafond</a>
 * @version 1.0
 */
class UnblockedSitesMonitor
{
    private static long MONITOR_SLEEP_DELAY_MS = 5l * 60l * 1000l;

    // private members ---------------------------------------------------------
    private final WebFilterBase wfb;
    private final Logger logger = Logger.getLogger(getClass());
    private final Monitor monitor = new Monitor();
    private final WorkerRunner workerRunner = new WorkerRunner(monitor, UvmContextFactory.context());

    // constructors -----------------------------------------------
    public UnblockedSitesMonitor(WebFilterBase myWfb)
    {
        logger.info("UnblockedSitesMonitor initializing");
        wfb = myWfb;
    }

    // class protected methods -----------------------------------------------
    void addUnblockedSite(InetAddress addr, String site)
    {
        monitor.addUnblockedSite(addr, site);
    }

    void start()
    {
        workerRunner.start();
    }

    void stop()
    {
        workerRunner.stop();
    }

    // inner classes ---------------------------------------------------------
    /**
     * The Monitor class actually takes care of periodically checking
     * host-unblocked sites, and removing them if they are expired.
     */
    private final class Monitor implements Worker
    {
        private SortedSet<UnblockedSite> unblockedSites = new TreeSet<UnblockedSite>();
    
        public synchronized void addUnblockedSite(InetAddress addr, String site)
        {
            UnblockedSite bs = new UnblockedSite(addr, site);
            if (unblockedSites.contains(bs))
                unblockedSites.remove(bs);  // to make sure creation time is the latest...
            unblockedSites.add(bs);

            logger.info("Adding unblock:" + bs);
        }

        public void start() {}

        public void stop() {}

        public void work() throws InterruptedException
        {
            Thread.sleep(MONITOR_SLEEP_DELAY_MS);
            
            if (unblockedSites.isEmpty())
                return;

            long expirationTime = System.currentTimeMillis() - (wfb.getSettings().getUnblockTimeout()*1000);

            /**
             * If the first site in the list is not yet expired and the list is order
             * There is no need to traverse the whole list
             */
            if (unblockedSites.first().creationTimeMillis > expirationTime)
               return;

            try {
                Map<InetAddress,List<String>> sitesToDelete = new HashMap<InetAddress,List<String>>();
                synchronized(this) {
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
                        if (sitesToDelete.containsKey(bs.addr))
                            sitesToDelete.get(bs.addr).add(bs.site);
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

    /**
     * A site unblocked by a specific host. Note that this class has a
     * natural ordering that is inconsistent with equals(): we
     * consider two UnblockedSites to be equal if the host and the URL
     * are equals, but we order them by creation time.
     */
    private final class UnblockedSite implements Comparable<UnblockedSite> 
    {
        private String site;
        private InetAddress addr;
        private long creationTimeMillis;
    
        public UnblockedSite(InetAddress myAddr, String mySite)
        {
            site = mySite;
            addr = myAddr;
            creationTimeMillis = System.currentTimeMillis();
        }

        public int hashCode()
        {
            return (17 + 37 * (site.hashCode() + addr.hashCode()));
        }
    
        public int compareTo(UnblockedSite other)
        {
            return (int)(creationTimeMillis - other.creationTimeMillis);
        }
    
        public String toString()
        {
            return "Unblock {" + addr + ", " + site + "}";
        }

    }
}