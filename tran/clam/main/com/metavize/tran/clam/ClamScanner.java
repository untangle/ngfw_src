/*
 * Copyright (c) 2003, 2005 Metavize Inc.
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

public class ClamScanner implements VirusScanner
{
    public static final String VERSION_ARG = "-V";

    private static final Logger logger = Logger.getLogger(ClamScanner.class.getName());
    private static final int timeout = 30000; /* XXX should be user configurable */
    
    public ClamScanner() {}

    public String getVendorName()
    {
        return "Clam";
    }

    public String getSigVersion()
    {
        String versionNumber = "unknown";
        String versionTimestamp = "unknown";

        try {
            String command = "clamdscan " + VERSION_ARG;
            Process  scanProcess = Runtime.getRuntime().exec(command);
            InputStream is  = scanProcess.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));

            String line;
            int i = -1;

            /**
             * Drain clamdscan output, one line like; 'ClamAV 0.87/1134/Fri Oct 14 01:07:44 2005'
             */
            try {
                if ((line = in.readLine()) != null) {
                    StringTokenizer st = new StringTokenizer(line, "/");
                    String str = null;

                    if (st.hasMoreTokens()) {
                        String clamVersion = st.nextToken();
                        if (st.hasMoreTokens()) {
                            versionNumber = st.nextToken();
                            if (st.hasMoreTokens()) {
                                versionTimestamp = st.nextToken();
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                logger.error("Scan Exception: " + e);
            }

            in.close();
            is.close();
            scanProcess.destroy(); // It should be dead already, just to be sure...
        }
        catch (java.io.IOException e) {
            logger.error("clamdscan version exception: " + e);
        }
        return versionNumber + " -- " + versionTimestamp;
    }


    public VirusScannerResult scanFile (String pathName)
    {
        ClamScannerLauncher scan = new ClamScannerLauncher(pathName);
        Thread thread = new Thread(scan);
        thread.start();
        
        return scan.waitFor(this.timeout);
    }
}
