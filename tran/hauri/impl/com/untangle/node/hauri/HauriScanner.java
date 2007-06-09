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
package com.untangle.node.hauri;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import com.untangle.node.virus.VirusScanner;
import com.untangle.node.virus.VirusScannerResult;
import org.apache.log4j.Logger;

public class HauriScanner implements VirusScanner
{
    public static final String VERSION_ARG = "-V";

    private final Logger logger = Logger.getLogger(getClass());
    private static final int timeout = 30000; /* XXX should be user configurable */

    public HauriScanner() {}

    public String getVendorName()
    {
        return "Hauri";
    }

    public String getSigVersion()
    {
        String version = "unknown";

        try {
            // Note that we do NOT use UvmContext.exec here because we run at
            // reports time where there is no UvmContext.
            Process scanProcess = Runtime.getRuntime().exec("virobot " + VERSION_ARG);
            InputStream is  = scanProcess.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));

            String line;
            int i = -1;

            /**
             * Drain virobot output, one line like; 'Engine Version : 2005-10-14'
             */
            try {
                if ((line = in.readLine()) != null) {
                    StringTokenizer st = new StringTokenizer(line, ":");
                    String str = null;

                    if (st.hasMoreTokens()) {
                        String label = st.nextToken();
                        if (st.hasMoreTokens()) {
                            version = st.nextToken();
                        }
                    }
                }
            }
            catch (Exception e) {
                logger.error("Scan Exception: ", e);
            }

            in.close();
            is.close();
            scanProcess.destroy(); // It should be dead already, just to be sure...
        }
        catch (java.io.IOException e) {
            logger.error("virobot version exception: ", e);
        }
        return version;
    }

    public VirusScannerResult scanFile(File scanfile)
    {
        HauriScannerLauncher scan = new HauriScannerLauncher(scanfile);
        return scan.doScan(this.timeout);
    }
}
