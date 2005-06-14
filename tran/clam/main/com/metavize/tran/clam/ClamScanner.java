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

import org.apache.log4j.Logger;

import com.metavize.tran.virus.VirusScanner;
import com.metavize.tran.virus.VirusScannerResult;

public class ClamScanner implements VirusScanner {

    private static final Logger logger = Logger.getLogger(ClamScanner.class.getName());

    /**
     * These are "FOUND" viruses by clamdscan, but are not really viruses
     * copied from the clam source
     */
    private static final String[] invalidVirusNames = { "Encrypted.RAR",
                                                        "Oversized.RAR",
                                                        "RAR.ExceededFileSize",
                                                        "RAR.ExceededFilesLimit",
                                                        "Suspect.Zip",
                                                        "Exploit.Zip.ModifiedHeaders",
                                                        "Oversized.Zip",
                                                        "Encrypted.Zip",
                                                        "Zip.ExceededFileSize",
                                                        "Zip.ExceededFilesLimit",
                                                        "GZip.ExceededFileSize",
                                                        "BZip.ExceededFileSize",
                                                        "MSCAB.ExceededFileSize",
                                                        "Archive.ExceededRecursionLimit" };
                                           
    
    public ClamScanner() {}

    public VirusScannerResult scanFile (String pathName)
        throws IOException, InterruptedException
    {
        /* Clam clamdscan can process special files (archives/zip/etc) and
         * by default, it handles these special files
         */
        Process proc = Runtime.getRuntime().exec("nice -n 19 clamdscan " + pathName);
        InputStream is  = proc.getInputStream();
        OutputStream os = proc.getOutputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        os.close();

        String virusName = null;
        String s;
        int i;

        /**
         * Drain clamdscan output
         */
        while ((s = in.readLine()) != null) {
            /**
             * This returns the 2nd word if " FOUND" is present:
             *
             * clamdscan found output:
             * /home/dmorris/q347558.exe: Worm.Gibe.F FOUND
             *
             * ----------- SCAN SUMMARY -----------
             * Infected files: 1
             * Time: 0.016 sec (0 m 0 s)
             *
             * clamdscan not found output:
             * /home/dmorris/foo: OK
             *
             * ----------- SCAN SUMMARY -----------
             * Infected files: 0
             * Time: 0.002 sec (0 m 0 s)
             */

            if (true == s.endsWith(" FOUND")) {
                StringTokenizer st = new StringTokenizer(s);
                String str = null;
                
                for (i = 0 ; true == st.hasMoreTokens() ; i++) {
                    str = st.nextToken();
                    if (1 == i) {
                        virusName = str;
                        break;
                    }
                }
            }
        }

        proc.waitFor();
        i = proc.exitValue();
        in.close();
        is.close();

        /**
         * PROGRAM EXIT CODES
         * 0 : NO virus found.
         * 1 : Virus(ese) found.
         * 2 : An error occured.
         */
        switch(i) {
        case 0:
            logger.info("clamdscan: clean");
            return VirusScannerResult.CLEAN;
        case 1:
            if (virusName == null) {
                logger.warn("clamdscan: missing \"FOUND\" string (exit code 1)");
                return VirusScannerResult.ERROR;
            } else {
                for (i=0 ; i<invalidVirusNames.length ; i++) {
                    if (virusName == invalidVirusNames[i]) {
                        logger.warn("clamdscan: " + i);
                        return VirusScannerResult.ERROR;
                    }
                }
                logger.info("clamdscan: infected (" + virusName + ")");
                return new VirusScannerResult(false,virusName,false);
            }
        default:
        case 2:
            logger.error("clamdscan exit code error: " + i);
            return VirusScannerResult.ERROR;
        }
    }

    public VirusScannerResult scanBufs(List bufs)
        throws IOException, InterruptedException
    {
        File fileBuf = File.createTempFile("clamdscan-cache",null);
        FileChannel outFile = (new FileOutputStream(fileBuf)).getChannel();

        ByteBuffer bb;

        for (int i = 0; i < bufs.size(); i++) {
            bb = (ByteBuffer)bufs.get(i);
            bb.flip();
            while (0 < bb.remaining())
                outFile.write(bb);
        }
        outFile.close();

        VirusScannerResult ret = scanFile(fileBuf.getAbsolutePath());
        fileBuf.delete();

        return ret;
    }
}
