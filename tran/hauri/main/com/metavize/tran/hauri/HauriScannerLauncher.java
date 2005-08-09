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
package com.metavize.tran.hauri;

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

public class HauriScannerLauncher implements Runnable
{
    private static final Logger logger = Logger.getLogger(HauriScannerLauncher.class.getName());

    private Process scanProcess = null;
    private String pathName = null;
    private VirusScannerResult result = null;

    /**
     * Create a Launcher for the give file
     */
    public HauriScannerLauncher(String pathName)
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
            logger.warn("Virus scan interrupted, killing virobot, assuming clean");
            this.scanProcess.destroy();
            return VirusScannerResult.ERROR;
        }

        if (this.result == null) {
            logger.warn("Timer expired, killing virobot, assuming clean");
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
            this.scanProcess = Runtime.getRuntime().exec("virobot " + pathName);
            InputStream is  = this.scanProcess.getInputStream();
            OutputStream os = this.scanProcess.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            os.close();

            String virusName = null;
            String s;
            int i = -1;

            /**
             * Drain virobot output
             */
            try {
                while ((s = in.readLine()) != null) {
                    /**
                     * virobot output
                     * "/home/dmorris/q347558.exe.2  Infection: W32/Swen.A@mm"
                     * This returns the 3rd word
                     */
                    if (s.startsWith(" Detected")) {
                        StringTokenizer st = new StringTokenizer(s);
                        String str = null;

                        for (i=0 ; st.hasMoreTokens() ; i++) {
                            str = st.nextToken();

                            if (i==1) {
                                virusName = str;
                                logger.warn("found: " + virusName + " in " + pathName);
                                break;
                            }
                        }
                    }
                }
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
             * 255    Error
             */
            switch(i) {
            case 0:
                logger.info("virobot: clean");
                this.result = VirusScannerResult.CLEAN;
                synchronized (this) {this.notifyAll();}
                return;
            case 1:
                if (virusName == null) {
                    logger.info("virobot: infected (unknown)");
                    this.result = VirusScannerResult.ERROR;
                    synchronized (this) {this.notifyAll();}
                    return;
                } else {
                    logger.info("virobot: infected (" + virusName + ")");
                    this.result = new VirusScannerResult(false,virusName,false);
                    synchronized (this) {this.notifyAll();}
                    return;
                }
            case 255:
                logger.error("virobot exit code error: " + i);
                this.result = VirusScannerResult.ERROR;
                synchronized (this) {this.notifyAll();}
                return;
            default:
                logger.error("virobot exit code error: " + i);
                this.result = VirusScannerResult.ERROR;
                synchronized (this) {this.notifyAll();}
                return;
            }
        }
        catch (java.io.IOException e) {
            logger.error("virobot scan exception: " + e);
            this.result = VirusScannerResult.ERROR;
            synchronized (this) {this.notifyAll();}
            return;
        }
        catch (java.lang.InterruptedException e) {
            logger.warn("virobot interrupted: " + e);
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
