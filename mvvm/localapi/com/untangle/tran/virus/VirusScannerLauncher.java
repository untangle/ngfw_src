/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.tran.virus;

import org.apache.log4j.Logger;
import org.apache.log4j.Logger;

import com.untangle.mvvm.MvvmContextFactory;
import org.apache.log4j.Logger;

abstract public class VirusScannerLauncher implements Runnable
{
    protected final Logger logger = Logger.getLogger(getClass());

    protected String pathName = null;

    // These next must be volatile since they are written and read by different threads.  bug948
    protected volatile Process scanProcess = null;
    protected volatile VirusScannerResult result = null;

    protected VirusScannerLauncher(String pathName)
    {
        this.pathName = pathName;
    }

    /**
     * Starts the scan and waits for timeout milliseconds for a result
     * If a result is reached, it is returned.
     * If the time expires VirusScannerResult.ERROR is returned
     */
    public VirusScannerResult doScan(int timeout)
    {
        Thread thread = MvvmContextFactory.context().newThread(this);
        long startTime = System.currentTimeMillis();
        try {
            synchronized (this) {
                // Don't start the thread until we have the monitor held.
                thread.start();

                this.wait(timeout);

                // FuckinA, Java can return from wait() spuriously!
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
            this.scanProcess.destroy();
            return VirusScannerResult.ERROR;
        }

        if (this.result == null) {
            logger.warn("Timer expired, killing process, assuming clean");

            /**
             * This is debugging information for bug 948
             */
            if (this.scanProcess == null) {
                logger.warn("ScannerLauncher Thread Status: " + thread.getState());
                logger.warn("ScannerLauncher Thread isAlive: " + thread.isAlive());
                logger.error("Virus process (" + getClass() + ") failed to launch.");
            } else {
                this.scanProcess.destroy();
            }

            return VirusScannerResult.ERROR;
        } else {
            return this.result;
        }
    }

    /**
     * retrieve the stored result, null if not set
     */
    public VirusScannerResult getResult()
    {
        return this.result;
    }

    abstract public void  run();
}
