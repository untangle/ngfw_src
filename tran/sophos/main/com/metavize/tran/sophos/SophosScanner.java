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
package com.metavize.tran.sophos;

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

public class SophosScanner implements VirusScanner
{
    private static final Logger logger = Logger.getLogger(SophosScanner.class.getName());

    public SophosScanner() {}

    public String getVendorName() {
        return "Sophos";
    }

    public VirusScannerResult scanFile (String fileName)
        throws IOException, InterruptedException
    {
        /* Sophos sweep can process special files (archives/zip/etc)
         * but by default,
         * Sophos sweep doesn't sweep these special files
         */
        Process proc = Runtime.getRuntime().exec("nice -n 19 sweep -archive " + fileName);
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
             * sweep output
             * ">>> Virus 'W32/Gibe-F' found in file q347558.exe"
             * This returns the 3rd word
             */
            if (s.startsWith(">>>")) {
                StringTokenizer st = new StringTokenizer(s);
                for (i=0 ; i<3 && st.hasMoreTokens() ; i++) {
                    String name = st.nextToken();
                    if (i==2) {
                        virusName=name;
                        break;
                    }
                }
            }
        }

        /**
         * PROGRAM EXIT CODES
         *   0      If no errors are encountered and no viruses are found.
         *   1      If the user interrupts SWEEP (usually by pressing control-C) or kills the process.
         *   2      If some error preventing further execution is discovered.
         *   3      If viruses or virus fragments are discovered.
         */
        proc.waitFor();
        i = proc.exitValue();
        in.close();
        is.close();

        switch(i) {
        case 0:
            logger.info("sweep: clean");
            return VirusScannerResult.CLEAN;
        case 3: 
            if (virusName == null) {
                logger.info("sweep: infected (unknown)");
                return VirusScannerResult.INFECTED;
            } else {
                logger.info("sweep: infected (" + virusName + ")");
                return new VirusScannerResult(false,virusName,false);
            }
        case 2:
        case 1:
            logger.error("sweep exit code error: " + i);
            return VirusScannerResult.ERROR;
        default:
            logger.error("Unknown sweep exit code: " + i);
            return VirusScannerResult.ERROR;
        }
    }

    public VirusScannerResult scanBufs(List bufs)
        throws IOException, InterruptedException
    {
        File fileBuf = File.createTempFile("sweep-cache",null);
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
