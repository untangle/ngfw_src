/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.clamphish;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.tran.virus.VirusScannerResult;
import com.metavize.tran.clam.ClamScannerLauncher;
import com.metavize.tran.spam.SpamScanner;
import com.metavize.tran.spam.SpamReport;
import com.metavize.tran.spam.ReportItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

class ClamPhishScanner implements SpamScanner
{
    // High enough to be a hit.  Should be elsewhere.  XXX
    public static final float HIT_SCORE = 100f;

    private final Logger logger = Logger.getLogger(ClamPhishScanner.class.getName());
    private static final int timeout = 10000; /* XXX should be user configurable */

    ClamPhishScanner() { }

    public String getVendorName()
    {
        return "Clam";
    }

    public SpamReport scanFile(File f, float threshold)
    {
        String filePath = f.getPath();
        if (logger.isDebugEnabled())
            logger.debug("scanning file " + filePath);
        ClamScannerLauncher scan = new ClamScannerLauncher(filePath);
        Thread thread = MvvmContextFactory.context().newThread(scan);
        thread.start();
        VirusScannerResult vsr = scan.waitFor(this.timeout);
        if (logger.isDebugEnabled())
            logger.debug("scan finished, clean: " + vsr.isClean() + ", name: " + vsr.getVirusName());
        if (vsr.isClean() || vsr.getVirusName() == null || !vsr.getVirusName().contains("Phish")) {
            return SpamReport.EMPTY;
        } else {
            ReportItem ourItem = new ReportItem(HIT_SCORE, vsr.getVirusName());
            List<ReportItem> items = new LinkedList<ReportItem>();
            items.add(ourItem);
            return new SpamReport(items, threshold);
        }
    }
}
