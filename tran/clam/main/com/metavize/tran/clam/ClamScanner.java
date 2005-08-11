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
    private static final Logger logger = Logger.getLogger(ClamScanner.class.getName());
    private static final int timeout = 30000; /* XXX should be user configurable */
    
    public ClamScanner() {}

    public String getVendorName()
    {
        return "Clam";
    }

    public VirusScannerResult scanFile (String pathName)
    {
        ClamScannerLauncher scan = new ClamScannerLauncher(pathName);
        Thread thread = new Thread(scan);
        thread.start();
        
        return scan.waitFor(this.timeout);
    }
}
