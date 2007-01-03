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
package com.untangle.tran.clam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.StringTokenizer;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.tran.virus.VirusScannerLauncher;
import com.untangle.tran.virus.VirusScannerResult;

public class ClamScannerLauncher extends VirusScannerLauncher
{
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
                                                        "Archive.ExceededRecursionLimit",
                                                        "PE.UPX.ExceededFileSize"};

    /**
     * Create a Launcher for the give file
     */
    public ClamScannerLauncher(String pathName)
    {
        super(pathName);
    }


    /**
     * This runs the virus scan, and stores the result for retrieval.
     * Any threads in waitFor() are awoken so they can retrieve the
     * result
     */
    public void run()
    {
        try {
            String command = "clamdscan " + pathName;
            this.scanProcess = MvvmContextFactory.context().exec(command);
            InputStream is  = scanProcess.getInputStream();
            OutputStream os = scanProcess.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            os.close();

            String virusName = null;
            String firstLine = null;
            String line;
            int i = -1;

            /**
             * Drain clamdscan output
             */
            try {
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

                while ((line = in.readLine()) != null) {
                    if (firstLine == null)
                        firstLine = line;

                    if (true == line.endsWith(" FOUND")) {
                        StringTokenizer st = new StringTokenizer(line);
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
            catch (java.io.IOException e) {
                /**
                 * This is only a warning because this happens when the process is killed because
                 * the timer expires
                 */
                logger.warn("Scan Exception: ", e);
                scanProcess.destroy();
                this.result = VirusScannerResult.CLEAN;
                return;
            }
            catch (Exception e) {
                logger.error("Scan Exception: ", e);
                scanProcess.destroy();
                this.result = VirusScannerResult.CLEAN;
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
                /**
                 * Clean
                 */
            case 0:
                logger.info("clamdscan: clean");
                this.result = VirusScannerResult.CLEAN;
                return;

                /**
                 * Virus found
                 */
            case 1:
                if (virusName == null) {
                    logger.error("clamdscan: missing \"FOUND\" string (exit code 1)");
                    this.result = VirusScannerResult.ERROR;
                    return;
                } else {
                    for (i=0 ; i<invalidVirusNames.length ; i++) {
                        if (virusName.equalsIgnoreCase(invalidVirusNames[i])) {
                            logger.warn("clamdscan: ignoring " + invalidVirusNames[i]);
                            this.result = VirusScannerResult.CLEAN;
                            return;
                        }
                    }
                    logger.info("clamdscan: infected (" + virusName + ")");
                    this.result = new VirusScannerResult(false,virusName,false);
                    return;
                }

                /**
                 * Killed
                 */
            case 143:
                logger.warn("clamdscan prematurely killed");
                this.result = VirusScannerResult.ERROR;
                return;

                /**
                 * Error
                 */
            default:
            case 2:
                String errorOut = null;
                try {
                    InputStream es  = scanProcess.getErrorStream();
                    int numbytes = es.available();
                    byte[] b = new byte[numbytes];
                    es.read(b,0,numbytes);
                    errorOut = new String(b);
                } catch (Exception e) {
                    logger.warn("Exception reading Error output: " + e);
                }

                logger.error("clamdscan exit code error: " + i +
                             " \nCommand: " + command +
                             " \nfirstLine output: " + firstLine +
                             " \nerror     output: " + errorOut);
                this.result = VirusScannerResult.ERROR;
                return;
            }
        }
        catch (java.io.IOException e) {
            logger.error("clamdscan scan exception: ", e);
            this.result = VirusScannerResult.ERROR;
            return;
        }
        catch (java.lang.InterruptedException e) {
            logger.warn("clamdscan interrupted: ", e);
            this.result = VirusScannerResult.ERROR;
            return;
        }
        catch (Exception e) {
            logger.warn("clamdscan exception: ", e);
            this.result = VirusScannerResult.ERROR;
            return;
        }
        finally {
            synchronized (this) {this.notifyAll();}
        }
    }
}
