/**
 * $Id$
 */

package com.untangle.app.virus_blocker;

import java.io.File;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.AppSession;

/**
 * Virus Blocker Scanner Launcher
 */
abstract public class VirusScannerLauncher implements Runnable
{
    protected final Logger logger = Logger.getLogger(getClass());

    protected String scanfilePath = null;
    protected AppSession appSession = null;

    // This must be volatile since they are written and read by different threads.  bug948
    protected volatile VirusScannerResult result = null;

    /**
     * Constructor
     * 
     * @param scanfile
     *        The file to be scanned
     * @param session
     *        The application session
     */
    protected VirusScannerLauncher(File scanfile, AppSession session)
    {
        if (scanfile != null) scanfilePath = scanfile.getAbsolutePath();
        appSession = session;
    }

    /**
     * 
     * Starts the scan and waits for timeout milliseconds for a result If a
     * result is reached, it is returned. If the time expires
     * VirusScannerResult.ERROR is returned
     * 
     * @param timeout
     *        The scan timeout
     * @return The scan result
     */
    public VirusScannerResult doScan(long timeout)
    {
        Thread thread = UvmContextFactory.context().newThread(this);
        long startTime = System.currentTimeMillis();
        try {
            synchronized (this) {
                // Don't start the thread until we have the monitor held.
                thread.start();

                this.wait(timeout);

                // Argh! Java can return from wait() spuriously!
                if (this.result == null) {
                    long currentTime = System.currentTimeMillis();
                    while (this.result == null && (currentTime - startTime) < timeout) {
                        this.wait(timeout - (currentTime - startTime));
                        currentTime = System.currentTimeMillis();
                    }
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Virus scan interrupted, killing process, assuming clean");
            return VirusScannerResult.ERROR;
        }

        if (this.result == null) {
            logger.warn("Timer expired, killing process, assuming clean");

            return VirusScannerResult.ERROR;
        } else {
            return this.result;
        }
    }

    /**
     * The run function
     */
    abstract public void run();
}
