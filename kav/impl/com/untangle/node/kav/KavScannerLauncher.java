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
package com.untangle.node.kav;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.StringTokenizer;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.node.virus.VirusScannerLauncher;
import com.untangle.node.virus.VirusScannerResult;

public class KavScannerLauncher extends VirusScannerLauncher
{
    /**
     * Create a Launcher for the give file
     */
    public KavScannerLauncher(File scanfile)
    {
        super(scanfile);
    }

    /**
     * This runs the virus scan, and stores the result for retrieval.
     * Any threads in waitFor() are awoken so they can retrieve the
     * result
     */
    public void run()
    {
        try {
            String command = "kavclient " + scanfilePath;
            this.scanProcess = UvmContextFactory.context().exec(command);
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
                            logger.warn("found: " + virusName + " in " + scanfilePath);
                        }
                    }
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("kavclient output: " + wholeOutput.toString());
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
                this.scanProcess.destroy();
                this.result = VirusScannerResult.CLEAN;
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
                return;
            case 1:
                if (virusName == null) {
                    logger.error("kavclient: infected (unknown)");
                    this.result = VirusScannerResult.ERROR;
                    return;
                } else {
                    logger.info("kavclient: infected (" + virusName + ")");
                    this.result = new VirusScannerResult(false, virusName, false);
                    return;
                }
            case 2:
                logger.warn("kavclient exit code error: " + wholeOutput.toString());
                this.result = VirusScannerResult.ERROR;
                return;
            case 255:
                logger.error("kavclient exit code error: " + wholeOutput.toString());
                this.result = VirusScannerResult.ERROR;
                return;
            default:
                logger.error("kavclient exit code error code: " + i + ", error: " + wholeOutput.toString());
                this.result = VirusScannerResult.ERROR;
                return;
            }
        }
        catch (java.io.IOException e) {
            logger.error("kavclient scan exception: ", e);
            this.result = VirusScannerResult.ERROR;
            return;
        }
        catch (java.lang.InterruptedException e) {
            logger.warn("kavclient interrupted: ", e);
            this.result = VirusScannerResult.ERROR;
            return;
        }
        catch (Exception e) {
            logger.warn("kavclient exception: ", e);
            this.result = VirusScannerResult.ERROR;
            return;
        }
        finally {
            synchronized (this) {this.notifyAll();}
        }
    }
}
