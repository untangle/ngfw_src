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

package com.metavize.tran.spamassassin;

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

class SpamAssassinScanner implements SpamScanner
{
    private final Logger logger = Logger.getLogger(SpamAssassinScanner.class.getName());
    private static final int timeout = 20000; /* XXX should be user configurable */

    SpamAssassinScanner() { }

    public String getVendorName()
    {
        return "SpamAssassin";
    }

    public SpamReport scanFile(File f, float threshold)
    {
        SpamAssassinScannerLauncher scan = new SpamAssassinScannerLauncher(f, threshold);
        return scan.doScan(this.timeout);
    }
}
