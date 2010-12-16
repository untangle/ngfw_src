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

package com.untangle.node.phish;

import java.io.File;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.untangle.node.clam.ClamScannerClientLauncher;
import com.untangle.node.spam.ReportItem;
import com.untangle.node.spam.SpamReport;
import com.untangle.node.spam.SpamScanner;
import com.untangle.node.virus.VirusScannerResult;
import com.untangle.uvm.node.script.ScriptRunner;
import org.apache.log4j.Logger;

public class PhishScanner implements SpamScanner
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final float HIT_SCORE = 100f;
    private static final int TIMEOUT = 10000;

    private static int activeScanCount = 0;
    private static Object activeScanMonitor = new Object();

    private static final String GET_LAST_SIGNATURE_UPDATE = System.getProperty( "uvm.bin.dir" ) + "/phish-get-last-update";
    private static final String GET_LAST_SIGNATURE_UPDATE_CHECK = System.getProperty( "uvm.bin.dir" ) + "/phish-get-last-update-check";

    public PhishScanner() { }

    public String getVendorName()
    {
        return "Clam"; // also referenced in SpamSpamFilter
    }

    public int getActiveScanCount()
    {
        synchronized(activeScanMonitor) {
            return activeScanCount;
        }
    }

    public SpamReport scanFile(File msgFile, float threshold)
    {
        ClamScannerClientLauncher scan = new ClamScannerClientLauncher(msgFile);
        try {
            synchronized(activeScanMonitor) {
                activeScanCount++;
            }
            VirusScannerResult vsr = scan.doScan(PhishScanner.TIMEOUT);
            SpamReport result;
            if (vsr.isClean() || vsr.getVirusName() == null || !vsr.getVirusName().contains("Phish")) {
                result = SpamReport.EMPTY;
            } else {
                // convert VirusScannerResult to phish SpamReport
                ReportItem ourItem = new ReportItem(HIT_SCORE + threshold, vsr.getVirusName());
                List<ReportItem> items = new LinkedList<ReportItem>();
                items.add(ourItem);
                result = new SpamReport(items, threshold);
            }
            logger.debug("phishc: " + result);
            return result;
        } finally {
            synchronized(activeScanMonitor) {
                activeScanCount--;
            }
        }
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

    public Date getLastSignatureUpdateCheck()
    {
        try {
            String result = ScriptRunner.getInstance().exec( GET_LAST_SIGNATURE_UPDATE_CHECK );
            long timeSeconds = Long.parseLong( result.trim());

            return new Date( timeSeconds * 1000l );
        } catch ( Exception e ) {
            logger.warn( "Unable to get last update check.", e );
            return null;
        } 
    }
    
    public String getSignatureVersion()
    {
        /* This is currently not displayed in the UI or reports */
        return "";
    }
}
