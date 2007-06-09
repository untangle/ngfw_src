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
import java.util.StringTokenizer;

import com.untangle.node.virus.VirusScanner;
import com.untangle.node.virus.VirusScannerResult;
import org.apache.log4j.Logger;

public class KavScanner implements VirusScanner
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String VERSION_ARG = "-V";

    private static final int timeout = 30000; /* XXX should be user configurable */

    public KavScanner() {}

    public String getVendorName()
    {
        return "Kaspersky";
    }

    public String getSigVersion()
    {
        String version = "unknown";

        try {
            // Note that we do NOT use UvmContext.exec here because we run at
            // reports time where there is no UvmContext.
            Process scanProcess = Runtime.getRuntime().exec("kavclient " + VERSION_ARG);
            InputStream is  = scanProcess.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));

            String line;
            int i = -1;

            /**
             * Drain kavclient output, one line like; 'KAV 5.5.0/RELEASE : 2005-09-09'
             */
            try {
                if ((line = in.readLine()) != null) {
                    StringTokenizer st = new StringTokenizer(line, ":");
                    String str = null;

                    if (st.hasMoreTokens()) {
                        String kavlabel = st.nextToken();
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
            logger.error("kavclient version exception: ", e);
        }
        return version;
    }

    public VirusScannerResult scanFile(File scanfile)
    {
        KavScannerLauncher scan = new KavScannerLauncher(scanfile);
        return scan.doScan(this.timeout);
    }
}
