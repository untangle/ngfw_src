/**
 * $Id$
 */

package com.untangle.app.spam_blocker;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.untangle.uvm.UvmContextFactory;

import org.apache.log4j.Logger;

/**
 * This class actually implements the DnsBL lookup It launches a new thread,
 * because there is no easy way to specify a timeout of InetAddress.getByName()
 * which is the DNS mechanism used to do the DNSBL lookup.
 */
public final class DnsblClient implements Runnable
{
    private final Logger logger = Logger.getLogger(getClass());

    private Thread myThread = null;
    private String dbgName; // thread name and socket host

    private String hostname;
    private String ipAddr;
    private String invertedIPAddr;

    private volatile Boolean isBlacklisted = null;

    /**
     * Constructor
     * 
     * @param hostname
     *        The hostname
     * @param ipAddr
     *        The IP address
     * @param invertedIPAddr
     *        The inverted IP address
     */
    public DnsblClient(String hostname, String ipAddr, String invertedIPAddr)
    {
        this.hostname = hostname;
        this.ipAddr = ipAddr;
        this.invertedIPAddr = invertedIPAddr;
    }

    /**
     * Start the scan
     */
    public void startScan()
    {
        if (myThread != null) {
            logger.warn("Thread already exist!");
        } else {
            //logger.debug("start, thread: " + myThread + ", this: " + this);

            this.myThread = UvmContextFactory.context().newThread(this);
            this.myThread.start(); // execute run() now
        }
        return;
    }

    /**
     * Check the scan progress
     * 
     * @param timeout
     *        The timeout
     */
    public void checkProgress(long timeout)
    {
        //logger.debug("check, thread: " + myThread + ", this: " + this);
        if (myThread == null || !myThread.isAlive()) {
            logger.debug(dbgName + ", is not alive; not waiting");
            return;
        }

        try {
            synchronized (this) {
                long startTime = System.currentTimeMillis();
                this.wait(timeout); // wait for run() to finish/timeout

                // retry when no result yet and time remains before timeout
                long elapsedTime = System.currentTimeMillis() - startTime;
                while (null == this.getResult() && elapsedTime < timeout) {
                    this.wait(timeout - elapsedTime);
                    elapsedTime = System.currentTimeMillis() - startTime;
                }
            }
        } catch (InterruptedException e) {
            logger.warn(dbgName + ", DNSBL check interrupted", e);
        } catch (Exception e) {
            logger.warn(dbgName + ", DNSBL check failed", e);
        }

        if (null == this.getResult()) {
            logger.warn(dbgName + ", DNSBL check timer expired");
            stopScan();
        }

        return;
    }

    /**
     * Stop the scan
     */
    public void stopScan()
    {
        //logger.debug("stop, thread: " + myThread + ", this: " + this);
        if (myThread == null || !myThread.isAlive()) {
            logger.debug(dbgName + ", is not alive; no need to stop");
            return;
        }

        myThread.interrupt(); // stop run() now
        return;
    }

    /**
     * Get our string representation
     * 
     * @return String representation
     */
    public String toString()
    {
        return dbgName;
    }

    /**
     * Get the hostname
     * 
     * @return The hostname
     */
    public String getHostname()
    {
        return hostname;
    }

    /**
     * Get the IP address
     * 
     * @return The IP address
     */
    public String getIPAddr()
    {
        return ipAddr;
    }

    /**
     * Get the inverted IP address
     * 
     * @return The inverted IP address
     */
    public String getInvertedIPAddr()
    {
        return invertedIPAddr;
    }

    /**
     * Set the result
     * 
     * @param isBlacklisted
     *        The result
     */
    public void setResult(Boolean isBlacklisted)
    {
        this.isBlacklisted = isBlacklisted;
        return;
    }

    /**
     * Get the result
     * 
     * @return The result
     */
    public Boolean getResult()
    {
        return isBlacklisted;
    }

    /**
     * run() performs minimal work so if interrupted, it exits "immediately" ->
     * e.g., no need to implement stop flag
     */
    public void run()
    {
        Boolean isBlacklisted = Boolean.FALSE;

        try {
            InetAddress hostIPAddr = InetAddress.getByName(this.getInvertedIPAddr() + "." + this.getHostname() + ".");
            if (null != hostIPAddr) {
                logger.debug(dbgName + ", received confirmation that IP is on blacklist");
                isBlacklisted = Boolean.TRUE;
            } else {
                // assume ipAddr is not on this blacklist
                logger.debug(dbgName + ", could not confirm that IP is on blacklist; assuming IP is not on blacklist");
            }
        } catch (UnknownHostException e) {
            // ipAddr is not on this blacklist
            logger.debug(dbgName + ", received confirmation that IP is not on blacklist");
            // note that on interrupt,
            // IOException is thrown as UnknownHostException and
            // UnknownHostException will catch both so
            // we will not be able to differentiate between them
        } catch (SecurityException e) {
            // assume ipAddr is not on this blacklist
            logger.warn(dbgName + ", not allowed to query host: ", e);
        } catch (Exception e) {
            // assume ipAddr is not on this blacklist
            logger.warn(dbgName + ", DNSBL checker failed: ", e);
        } finally {
            this.setResult(isBlacklisted);

            synchronized (this) {
                this.notifyAll(); // notify waiting thread and finish run()
            }
        }
    }
}
