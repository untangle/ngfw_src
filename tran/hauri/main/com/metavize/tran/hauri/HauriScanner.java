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
package com.metavize.tran.hauri;

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

import org.apache.log4j.Logger;

import com.metavize.tran.virus.VirusScanner;
import com.metavize.tran.virus.VirusScannerResult;

public class HauriScanner implements VirusScanner
{
    private static final Logger logger = Logger.getLogger(HauriScanner.class.getName());
    private static final int timeout = 30000; /* XXX should be user configurable */

    public HauriScanner() {}

    public String getVendorName()
    {
        return "Hauri";
    }

    public VirusScannerResult scanFile (String pathName)
    {
        HauriScannerLauncher scan = new HauriScannerLauncher(pathName);
        Thread thread = new Thread(scan);
        thread.start();
        
        return scan.waitFor(this.timeout);
    }

    public VirusScannerResult scanBufs(List bufs)
    {
        File fileBuf;
        try {
            fileBuf = File.createTempFile("virobot-cache",null);
            FileChannel outFile = (new FileOutputStream(fileBuf)).getChannel();

            ByteBuffer bb;

            for (int i = 0; i < bufs.size(); i++) {
                bb = (ByteBuffer)bufs.get(i);
                bb.flip();
                while (bb.remaining()>0)
                    outFile.write(bb);
            }
            outFile.close();
        } catch (Exception x) {
            logger.error("virobot: unable to write file to be scanned", x);
            return VirusScannerResult.ERROR;
        }

        VirusScannerResult ret = scanFile(fileBuf.getAbsolutePath());
        try {
            fileBuf.delete();
        } catch (Exception x) {
            logger.error("clamdscan: unable to delete scanned file", x);
        }

        return ret;
    }
}
