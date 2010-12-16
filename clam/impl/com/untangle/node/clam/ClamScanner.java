/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.clam;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.StringTokenizer;

import com.untangle.uvm.node.script.ScriptRunner;

import com.untangle.node.virus.VirusScanner;
import com.untangle.node.virus.VirusScannerResult;
import org.apache.log4j.Logger;

public class ClamScanner implements VirusScanner
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final int timeout = 29500; /* XXX should be user configurable */

    private static final String GET_LAST_SIGNATURE_UPDATE = System.getProperty( "uvm.bin.dir" ) + "/clam-get-last-update";

    private static final String VERSION_ARG = "-V";

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
            // Note that we do NOT use UvmContext.exec here because we run at
            // reports time where there is no UvmContext.
            Process scanProcess = Runtime.getRuntime().exec(command);
            InputStream is  = scanProcess.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));

            String line;

            /**
             * Drain clamdscan output, one line like; 'ClamAV 0.87/1134/Fri Oct 14 01:07:44 2005'
             */
            try {
                if ((line = in.readLine()) != null) {
                    StringTokenizer st = new StringTokenizer(line, "/");

                    if (st.hasMoreTokens()) {
                        @SuppressWarnings("unused")
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
                logger.error("Scan Exception: ", e);
            }

            in.close();
            is.close();
            scanProcess.destroy(); // It should be dead already, just to be sure...
        }
        catch (java.io.IOException e) {
            logger.error("clamdscan version exception: ", e);
        }
        return versionNumber + " -- " + versionTimestamp;
    }

    public VirusScannerResult scanFile(File scanfile)
    {
        ClamScannerClientLauncher scan = new ClamScannerClientLauncher(scanfile);
        return scan.doScan(timeout);
    }

    
    public Date getLastSignatureUpdate()
    {
        try {
            String result = ScriptRunner.getInstance().exec( GET_LAST_SIGNATURE_UPDATE );
            long timeSeconds = Long.parseLong( result.trim());

            return new Date( timeSeconds * 1000l );
        } catch ( Exception e ) {
            logger.warn( "Unable to get last update.", e );
            return null;
        } 
    }
}
