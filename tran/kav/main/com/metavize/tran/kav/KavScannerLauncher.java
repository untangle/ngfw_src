/*
 * Copyright (c) 2004,2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.kav;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.StringTokenizer;

import com.metavize.tran.util.AlarmTimer;
import com.metavize.tran.virus.VirusScanner;
import com.metavize.tran.virus.VirusScannerResult;
import org.apache.log4j.Logger;

public class KavScannerLauncher implements Runnable
{
    private static final Logger logger = Logger.getLogger(KavScannerLauncher.class.getName());

    private Process scanProcess = null;
    private String pathName = null;
    private VirusScannerResult result = null;

    /**
     * Create a Launcher for the give file
     */
    public KavScannerLauncher(String pathName)
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
        catch (java.lang.InterruptedException e) {
            logger.warn("Virus scan interrupted, killing kavclient, assuming clean");
            this.scanProcess.destroy();
            return VirusScannerResult.ERROR;
        }

        if (this.result == null) {
            logger.warn("Timer expired, killing kavclient, assuming clean");
            this.scanProcess.destroy();
            return VirusScannerResult.ERROR;
        }
        else {
            return this.result;
        }
    }

    /**
     * This runs the virus scan, and stores the result for retrieval.
     * Any threads in waitFor() are awoken so they can retrieve the
     * result
     */
    public void run()
    {
        if (result != null) {
            /* already done */
            return;
        }
            
        try {
            String command = "kavclient " + pathName;
            this.scanProcess = Runtime.getRuntime().exec(command);
            InputStream is  = scanProcess.getInputStream();
            OutputStream os = scanProcess.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            os.close();

            String virusName = null;
            StringBuilder wholeOutput = new StringBuilder();
            String s;
            int i = -1;

            /**
             * Drain kavclient output
             */
            try {
                /**
                 * kavclient output:
                 * $ kavclient /bin/sh
                 * Clean
                 * $ kavclient /tmp/q347558.exe
                 * Infected: Email-Worm.Win32.Swen /
                 * 
                 */

                while ((s = in.readLine()) != null) {
                    wholeOutput.append(s);
                     /*
                     * This returns the 2nd word
                     */
                    if (s.startsWith("Infected:")) {
                        StringTokenizer st = new StringTokenizer(s);
                        st.nextToken();
                        if (st.hasMoreTokens()) {
                            virusName = st.nextToken();
                            logger.warn("found: " + virusName + " in " + pathName);
                        }
                    }
                }
                if (logger.isDebugEnabled())
                    logger.debug("kavclient output: " + wholeOutput.toString());
            }
            catch (Exception e) {
                logger.error("Scan Exception: " + e);
                this.scanProcess.destroy();
                this.result = VirusScannerResult.CLEAN;
                synchronized (this) {this.notifyAll();}
                return;
            }

            this.scanProcess.waitFor();
            i = this.scanProcess.exitValue();
            in.close();
            is.close();

            /**
             * PROGRAM EXIT CODES
             * 0      Normal exit.  Nothing found, nothing done.
             * 1      Virus Found
             * 2      Warning
             * 255    Error
             */
            switch(i) {
            case 0:
                logger.info("kavclient: clean");
                this.result = VirusScannerResult.CLEAN;
                synchronized (this) {this.notifyAll();}
                return;
            case 1:
                if (virusName == null) {
                    logger.error("kavclient: infected (unknown)");
                    this.result = VirusScannerResult.ERROR;
                    synchronized (this) {this.notifyAll();}
                    return;
                } else {
                    logger.info("kavclient: infected (" + virusName + ")");
                    this.result = new VirusScannerResult(false, virusName, false);
                    synchronized (this) {this.notifyAll();}
                    return;
                }
            case 2:
                logger.warn("kavclient exit code error: " + wholeOutput.toString());
                this.result = VirusScannerResult.ERROR;
                synchronized (this) {this.notifyAll();}
                return;
            case 255:
                logger.error("kavclient exit code error: " + wholeOutput.toString());
                this.result = VirusScannerResult.ERROR;
                synchronized (this) {this.notifyAll();}
                return;
            default:
                logger.error("kavclient exit code error code: " + i + ", error: " + wholeOutput.toString());
                this.result = VirusScannerResult.ERROR;
                synchronized (this) {this.notifyAll();}
                return;
            }
        }
        catch (java.io.IOException e) {
            logger.error("kavclient scan exception: " + e);
            this.result = VirusScannerResult.ERROR;
            synchronized (this) {this.notifyAll();}
            return;
        }
        catch (java.lang.InterruptedException e) {
            logger.warn("kavclient interrupted: " + e);
            this.result = VirusScannerResult.ERROR;
            synchronized (this) {this.notifyAll();}
            return;
        }
    }

    /**
     * retrieve the stored result, null if not set
     */
    public VirusScannerResult getResult()
    {
        return this.result;
    }
}
