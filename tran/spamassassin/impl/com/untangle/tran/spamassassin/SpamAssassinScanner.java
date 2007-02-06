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

package com.untangle.tran.spamassassin;

import com.untangle.tran.spam.SpamScanner;
import com.untangle.tran.spam.SpamReport;
import com.untangle.tran.spam.ReportItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

public class SpamAssassinScanner implements SpamScanner
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final int timeout = 40000; /* XXX should be user configurable */

    private static int activeScanCount = 0;
    private static Object activeScanMonitor = new Object();

    public SpamAssassinScanner() { }

    public String getVendorName()
    {
        return "SpamAssassin";
    }

    public int getActiveScanCount()
    {
        synchronized(activeScanMonitor) {
            return activeScanCount;
        }
    }

    public SpamReport scanFile(File msgFile, float threshold)
    {
        SpamAssassinScannerClientLauncher scan = new SpamAssassinScannerClientLauncher(msgFile, threshold);
        try {
            synchronized(activeScanMonitor) {
                activeScanCount++;
            }
            return scan.doScan(this.timeout);
        } finally {
            synchronized(activeScanMonitor) {
                activeScanCount--;
            }
        }
    }
}
