/**
 * $Id$
 */
package com.untangle.app.virus_blocker;

import org.apache.log4j.Logger;

abstract public class VirusClient implements Runnable
{
    protected final Logger logger = Logger.getLogger(getClass());

    protected final static int READ_SZ = 1024;

    protected final VirusClientContext cContext;

    protected Thread cThread;
    protected String dbgName; // thread name and socket host
    protected volatile boolean stop = false;

    public VirusClient(VirusClientContext cContext)
    {
        this.cContext = cContext;
    }

    public void setThread(Thread cThread)
    {
        this.cThread = cThread;
        dbgName = new StringBuilder("<").append(cThread.getName()).append(">").append(cContext.getHost()).append(":").append(cContext.getPort()).toString();
        return;
    }

    public void startScan()
    {
        //logger.debug("start, thread: " + cThread + ", this: " + this);
        cThread.start(); // execute run() now
        return;
    }

    // timeout > 0
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
                while ( cContext.getResult() == null && elapsedTime < timeout ) {
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

    public String toString()
    {
        return dbgName;
    }

    abstract public void run();

    public void cleanExit()
    {
        synchronized (this) {
            this.notifyAll(); // notify waiting thread and finish run()
            return;
        }
    }
}
