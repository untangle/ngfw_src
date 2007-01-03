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
import com.untangle.tran.clam.ClamScannerLauncher;
import com.untangle.tran.spam.ReportItem;
import com.untangle.tran.spam.SpamReport;
import com.untangle.tran.spam.SpamScanner;
import com.untangle.tran.virus.VirusScannerResult;
import org.apache.log4j.Logger;

class ClamPhishScanner implements SpamScanner
{
    // High enough to be a hit.  Should be elsewhere.  XXX
    public static final float HIT_SCORE = 100f;

    private final Logger logger = Logger.getLogger(ClamPhishScanner.class.getName());
    private static final int timeout = 10000; /* XXX should be user configurable */

    private static int activeScanCount = 0;
    private static Object activeScanMonitor = new Object();

    ClamPhishScanner() { }

    public String getVendorName()
    {
        return "Clam";
    }

    public int getActiveScanCount()
    {
        synchronized(activeScanMonitor) {
            return activeScanCount;
        }
    }

    public SpamReport scanFile(File f, float threshold)
    {
        try {
            synchronized(activeScanMonitor) {
                activeScanCount++;
            }
            String filePath = f.getPath();
            if (logger.isDebugEnabled()) {
                logger.debug("scanning file " + filePath);
            }
            ClamScannerLauncher scan = new ClamScannerLauncher(filePath);
            VirusScannerResult vsr = scan.doScan(this.timeout);
            if (logger.isDebugEnabled()) {
                logger.debug("scan finished, clean: " + vsr.isClean()
                             + ", name: " + vsr.getVirusName());
            }
            if (vsr.isClean() || vsr.getVirusName() == null || !vsr.getVirusName().contains("Phish")) {
                return SpamReport.EMPTY;
            } else {
                ReportItem ourItem = new ReportItem(HIT_SCORE, vsr.getVirusName());
                List<ReportItem> items = new LinkedList<ReportItem>();
                items.add(ourItem);
                return new SpamReport(items, threshold);
            }
        } finally {
            synchronized(activeScanMonitor) {
                activeScanCount--;
            }
        }
    }
}
