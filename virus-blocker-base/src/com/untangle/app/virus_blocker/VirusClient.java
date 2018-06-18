/**
 * $Id$
 */

package com.untangle.app.virus_blocker;

import org.apache.log4j.Logger;

/**
 * Abstract framework for a virus scanner client
 */
abstract public class VirusClient implements Runnable
{
    protected final Logger logger = Logger.getLogger(getClass());

    protected final static int READ_SZ = 1024;

    protected final VirusClientContext cContext;

    protected Thread cThread;
    protected String dbgName; // thread name and socket host
    protected volatile boolean stop = false;

    /**
     * Constructor
     * 
     * @param cContext
     *        The client context
     */
    public VirusClient(VirusClientContext cContext)
    {
        this.cContext = cContext;
    }

    /**
     * Sets the thread
     * 
     * @param cThread
     *        The thread
     */
    public void setThread(Thread cThread)
    {
        this.cThread = cThread;
        dbgName = new StringBuilder("<").append(cThread.getName()).append(">").append(cContext.getHost()).append(":").append(cContext.getPort()).toString();
        return;
    }

    /**
     * Starts the scan thread
     */
    public void startScan()
    {
        //logger.debug("start, thread: " + cThread + ", this: " + this);
        cThread.start(); // execute run() now
        return;
    }

    /**
     * Checks the progress. Timeout
     * 
     * @param timeout
     *        Timeout value > 0
     */
    public void checkProgress(long timeout)
    {
        //logger.debug("check, thread: " + cThread + ", this: " + this);
        if (false == cThread.isAlive()) {
            logger.debug(dbgName + ", is not alive; not waiting");
            return;
        }

        try {
            synchronized (this) {
                long startTime = System.currentTimeMillis();
                this.wait(timeout); // wait for run() to finish/timeout

                // retry when no result yet and time remains before timeout
                long elapsedTime = System.currentTimeMillis() - startTime;
                while (cContext.getResult() == null && elapsedTime < timeout) {
                    this.wait(timeout - elapsedTime);
                    elapsedTime = System.currentTimeMillis() - startTime;
                }
            }
        } catch (InterruptedException e) {
            logger.warn(dbgName + ", virusc interrupted", e);
        } catch (Exception e) {
            logger.warn(dbgName + ", virusc failed", e);
        }

        if (null == cContext.getResult()) {
            logger.warn(dbgName + ", virusc timer expired");
            stopScan();
        }

        return;
    }

    /**
     * Stop the scan thread
     */
    public void stopScan()
    {
        //logger.debug("stop, thread: " + cThread + ", this: " + this);
        if (false == cThread.isAlive()) {
            logger.debug(dbgName + ", is not alive; no need to stop");
            return;
        }

        this.stop = true;
        cThread.interrupt(); // stop run() now
        return;
    }

    /**
     * Create our string representation
     * 
     * @return Our string representation
     */
    public String toString()
    {
        return dbgName;
    }

    /**
     * The main run function
     */
    abstract public void run();

    /**
     * Exit the thread cleanly
     */
    public void cleanExit()
    {
        synchronized (this) {
            this.notifyAll(); // notify waiting thread and finish run()
            return;
        }
    }
}
