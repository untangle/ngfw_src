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
package com.untangle.tran.hauri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.StringTokenizer;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.tran.virus.VirusScannerLauncher;
import com.untangle.tran.virus.VirusScannerResult;

public class HauriScannerLauncher extends VirusScannerLauncher
{
    /**
     * Create a Launcher for the give file
     */
    public HauriScannerLauncher(String pathName)
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
            this.scanProcess = MvvmContextFactory.context().exec("virobot " + pathName);
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
                     * virobot output:
                     * -------------------------------------------------------------------------------
                     * ViRobot SDK Example ( Heuristic & Feature detection )        1 Feb 2002 Korea
                     * Copyright (c) 1998-2002 HAURI Inc.                        All rights reserved
                     * E-mail : hauri98@hauri.co.kr                                     Version 2.00
                     * -------------------------------------------------------------------------------
                     *
                     * Probing into q347558.exe
                     * Detected [I-Worm.Win32.Swen.106496] Virus - Recover ? (y/N)  <Virus Infected>
                     *
                     *
                     *
                     * Engine Version : 2005-09-15
                     *
                     * 1 Virus detected files.
                     */

                     /*
                     * This returns the 2nd word
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
             * 255    Error
             */
            switch(i) {
            case 0:
                logger.info("virobot: clean");
                this.result = VirusScannerResult.CLEAN;
                return;
            case 1:
                if (virusName == null) {
                    logger.info("virobot: infected (unknown)");
                    this.result = VirusScannerResult.ERROR;
                    return;
                } else {
                    logger.info("virobot: infected (" + virusName + ")");
                    this.result = new VirusScannerResult(false,virusName,false);
                    return;
                }
            case 255:
                logger.error("virobot exit code error: " + i);
                this.result = VirusScannerResult.ERROR;
                return;
            default:
                logger.error("virobot exit code error: " + i);
                this.result = VirusScannerResult.ERROR;
                return;
            }
        }
        catch (java.io.IOException e) {
            logger.error("virobot scan exception: ", e);
            this.result = VirusScannerResult.ERROR;
            return;
        }
        catch (java.lang.InterruptedException e) {
            logger.warn("virobot interrupted: ", e);
            this.result = VirusScannerResult.ERROR;
            return;
        }
        catch (Exception e) {
            logger.warn("virobot exception: ", e);
            this.result = VirusScannerResult.ERROR;
            return;
        }
        finally {
            synchronized (this) {this.notifyAll();}
        }
    }
}
