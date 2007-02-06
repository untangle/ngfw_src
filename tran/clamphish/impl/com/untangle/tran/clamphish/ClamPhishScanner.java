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

package com.untangle.tran.clamphish;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.tran.clam.ClamScannerClientLauncher;
import com.untangle.tran.spam.ReportItem;
import com.untangle.tran.spam.SpamReport;
import com.untangle.tran.spam.SpamScanner;
import com.untangle.tran.virus.VirusScannerResult;
import org.apache.log4j.Logger;

public class ClamPhishScanner implements SpamScanner
{
    private final Logger logger = Logger.getLogger(getClass());

    // High enough to be a hit.  Should be elsewhere.  XXX
    private static final float HIT_SCORE = 100f;
    /* XXX should be user configurable */
    private static final int timeout = 10000;

    private static int activeScanCount = 0;
    private static Object activeScanMonitor = new Object();

    public ClamPhishScanner() { }

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
        try {
            synchronized(activeScanMonitor) {
                activeScanCount++;
            }
            ClamScannerClientLauncher scan = new ClamScannerClientLauncher(msgFile);
            VirusScannerResult vsr = scan.doScan(this.timeout);
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
            logger.debug("clamphishc: " + result);
            return result;
        } finally {
            synchronized(activeScanMonitor) {
                activeScanCount--;
            }
        }
    }
}
