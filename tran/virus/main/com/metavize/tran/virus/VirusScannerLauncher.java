/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.virus;

import java.io.IOException;
import java.util.List;
import org.apache.log4j.Logger;

abstract public class VirusScannerLauncher implements Runnable
{
    protected static final Logger logger = Logger.getLogger(VirusScannerLauncher.class.getName());

    protected String pathName = null;

    // These next must be volatile since they are written and read by different threads.  bug948
    protected volatile Process scanProcess = null;
    protected volatile VirusScannerResult result = null;

    protected VirusScannerLauncher(String pathName)
    {
        this.pathName = pathName;
    }

    /**
     * Waits for timeout milliseconds for a result
     * If a result is reached, it is returned.
     * If the time expires VirusScannerResult.ERROR is returned
     */
    public VirusScannerResult waitFor(int timeout)
    {
        try {
            synchronized (this) {
                this.wait(timeout);
            }
        }
        catch (InterruptedException e) {
            logger.warn("Virus scan interrupted, killing process, assuming clean");
            this.scanProcess.destroy();
            return VirusScannerResult.ERROR;
        }

        if (this.result == null) {
            logger.warn("Timer expired, killing process, assuming clean");
            this.scanProcess.destroy();
            return VirusScannerResult.ERROR;
        }
        else {
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
