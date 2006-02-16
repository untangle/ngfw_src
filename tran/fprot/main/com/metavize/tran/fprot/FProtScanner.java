/*
 * Copyright (c) 2003, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.fprot;

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

import com.metavize.tran.virus.VirusScanner;
import com.metavize.tran.virus.VirusScannerResult;
import org.apache.log4j.Logger;

public class FProtScanner implements VirusScanner {

    private static final Logger logger = Logger.getLogger(FProtScanner.class.getName());

    public FProtScanner() {}

    public String getVendorName() {
        return "F-Prot";
    }

    public VirusScannerResult scanFile (String fileName)
        throws IOException, InterruptedException
    {
        /* F-Prot f-prot can process special files (archives/zip/etc) and
         * by default, it handles these special files
         */
        Process proc = MvvmContextFactory.context().exec("f-prot " + fileName);
        InputStream is  = proc.getInputStream();
        OutputStream os = proc.getOutputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        os.close();

        String virusName = null;
        String s;
        int i;

        /**
         * Drain sweep output
         */
        while ((s = in.readLine()) != null) {
            /**
             * f-prot output
             * "/home/dmorris/q347558.exe.2  Infection: W32/Swen.A@mm"
             * This returns the 3rd (last) word
             */
            if (s.startsWith("/")) {
                StringTokenizer st = new StringTokenizer(s);
                String last = null;

                for (i=0 ; st.hasMoreTokens() ; i++)
                    last = st.nextToken();

                virusName = last;
            }
        }

        /**
         * PROGRAM EXIT CODES
         * 0      Normal exit.  Nothing found, nothing done.
         * 1      Unrecoverable error (e.g., missing virus signature files).
         * 2      Selftest failed (program has been modified).
         * 3      At least one virus-infected object was found.
         * 4      Reserved, not currently in use.
         * 5      Abnormal termination (scanning did not finish).
         * 6      At least one virus was removed.
         * 7      Error, out of memory (should never happen, but well...)
         * 8      At least one suspicious object was found.
         */
        proc.waitFor();
        i = proc.exitValue();
        in.close();
        is.close();

        switch(i) {
        case 0:
            logger.info("f-prot: clean");
            return VirusScannerResult.CLEAN;
        case 3:
        case 8:
            if (virusName == null) {
                logger.info("f-prot: infected (unknown)");
                return VirusScannerResult.INFECTED;
            }
            else {
                logger.info("f-prot: infected (" + virusName + ")");
                return new VirusScannerResult(false,virusName,false);
            }
        case 6:
            return VirusScannerResult.CLEAN;
        case 1:
        case 2:
        case 4:
        case 5:
        case 7:
            logger.error("f-prot exit code error: " + i);
            return VirusScannerResult.ERROR;
        default:
            logger.error("Unknown f-prot exit code: " + i);
            return VirusScannerResult.ERROR;
        }
    }

    public VirusScannerResult scanBufs(List bufs)
        throws IOException, InterruptedException
    {
        File fileBuf = File.createTempFile("f-prot-cache",null);
        FileChannel outFile = (new FileOutputStream(fileBuf)).getChannel();

        ByteBuffer bb;

        for (int i = 0; i < bufs.size(); i++) {
            bb = (ByteBuffer)bufs.get(i);
            bb.flip();
            while (bb.remaining()>0)
                outFile.write(bb);
        }
        outFile.close();

        VirusScannerResult ret = scanFile(fileBuf.getAbsolutePath());
        fileBuf.delete();

        return ret;
    }
}
