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
package com.metavize.tran.clam;

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

public class ClamScannerLauncher implements Runnable
{
    private static final Logger logger = Logger.getLogger(ClamScannerLauncher.class.getName());

    private Process scanProcess = null;
    private String pathName = null;
    private VirusScannerResult result = null;

    /**
     * These are "FOUND" viruses by clamdscan, but are not really viruses
     * copied from the clam source
     */
    private static final String[] invalidVirusNames = { "Encrypted.RAR",
                                                        "Oversized.RAR",
                                                        "RAR.ExceededFileSize",
                                                        "RAR.ExceededFilesLimit",
                                                        "Suspect.Zip",
                                                        "Broken.Executable",
                                                        "Exploit.Zip.ModifiedHeaders",
                                                        "Oversized.Zip",
                                                        "Encrypted.Zip",
                                                        "Zip.ExceededFileSize",
                                                        "Zip.ExceededFilesLimit",
                                                        "GZip.ExceededFileSize",
                                                        "BZip.ExceededFileSize",
                                                        "MSCAB.ExceededFileSize",
                                                        "Archive.ExceededRecursionLimit" };


    /**
     * Create a Launcher for the give file
     */
    public ClamScannerLauncher(String pathName)
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
            logger.warn("Virus scan interrupted, killing clamdscan, assuming clean");
            this.scanProcess.destroy();
            return VirusScannerResult.ERROR;
        }

        if (this.result == null) {
            logger.warn("Timer expired, killing clamdscan, assuming clean");
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
            this.scanProcess = Runtime.getRuntime().exec("nice -n 19 clamdscan " + pathName);
            InputStream is  = scanProcess.getInputStream();
            OutputStream os = scanProcess.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            os.close();

            String virusName = null;
            String s;
            int i = -1;

            /**
             * Drain clamdscan output
             */
            try {
                while ((s = in.readLine()) != null) {
                    /**
                     * This returns the 2nd word if " FOUND" is present:
                     *
                     * clamdscan found output:
                     * /home/dmorris/q347558.exe: Worm.Gibe.F FOUND
                     *
                     * ----------- SCAN SUMMARY -----------
                     * Infected files: 1
                     * Time: 0.016 sec (0 m 0 s)
                     *
                     * clamdscan not found output:
                     * /home/dmorris/foo: OK
                     *
                     * ----------- SCAN SUMMARY -----------
                     * Infected files: 0
                     * Time: 0.002 sec (0 m 0 s)
                     */

                    if (true == s.endsWith(" FOUND")) {
                        StringTokenizer st = new StringTokenizer(s);
                        String str = null;

                        for (i = 0 ; true == st.hasMoreTokens() ; i++) {
                            str = st.nextToken();
                            if (1 == i) {
                                virusName = str;
                                break;
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                logger.error("Scan Exception: " + e);
                scanProcess.destroy();
                this.result = VirusScannerResult.CLEAN;
                synchronized (this) {this.notifyAll();}
                return;
            }

            scanProcess.waitFor();
            i = scanProcess.exitValue();
            in.close();
            is.close();

            /**
             * PROGRAM EXIT CODES
             * 0 : No virus found.
             * 1 : Virus(ese) found.
             * 2 : An error occured.
             */
            switch(i) {
            case 0:
                logger.info("clamdscan: clean");
                this.result = VirusScannerResult.CLEAN;
                synchronized (this) {this.notifyAll();}
                return;
            case 1:
                if (virusName == null) {
                    logger.error("clamdscan: missing \"FOUND\" string (exit code 1)");
                    this.result = VirusScannerResult.ERROR;
                    synchronized (this) {this.notifyAll();}
                    return;
                } else {
                    for (i=0 ; i<invalidVirusNames.length ; i++) {
                        if (virusName.equalsIgnoreCase(invalidVirusNames[i])) {
                            logger.warn("clamdscan: " + i);
                            this.result = VirusScannerResult.ERROR;
                            synchronized (this) {this.notifyAll();}
                            return;
                        }
                    }
                    logger.info("clamdscan: infected (" + virusName + ")");
                    this.result = new VirusScannerResult(false,virusName,false);
                    synchronized (this) {this.notifyAll();}
                    return;
                }
            case 143:
                logger.warn("clamdscan prematurely killed");
                this.result = VirusScannerResult.ERROR;
                synchronized (this) {this.notifyAll();}
                return;
            default:
            case 2:
                logger.error("clamdscan exit code error: " + i);
                this.result = VirusScannerResult.ERROR;
                synchronized (this) {this.notifyAll();}
                return;
            }
        }
        catch (java.io.IOException e) {
            logger.error("clamdscan scan exception: " + e);
            this.result = VirusScannerResult.ERROR;
            synchronized (this) {this.notifyAll();}
            return;
        }
        catch (java.lang.InterruptedException e) {
            logger.warn("clamdscan interrupted: " + e);
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
