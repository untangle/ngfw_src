/*
 * Copyright (c) 2003, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ClamScanner.java,v 1.4 2005/03/11 03:33:48 cng Exp $
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

    public ClamScanner() {}

    public VirusScannerResult scanFile (String pathName)
        throws IOException, InterruptedException
    {
        /* Clam  SDK can process some special files (zip/etc) and
         * by default, it handles these special files
         */
        Process proc = Runtime.getRuntime().exec("nice -n 19 clamdscan " + pathName);
        InputStream is  = proc.getInputStream();
        OutputStream os = proc.getOutputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        os.close();

        String virusName = null;
        String s;
        int i = 0;

        /**
         * Drain sweep output
         */
        while ((s = in.readLine()) != null) {
            /**
             * clam found output:
             * /home/dmorris/q347558.exe: Worm.Gibe.F FOUND
             *
             * ----------- SCAN SUMMARY -----------
             * Infected files: 1
             * Time: 0.016 sec (0 m 0 s)
             */
            /**
             * clam not found output:
             * /home/dmorris/foo: OK
             *
             * ----------- SCAN SUMMARY -----------
             * Infected files: 0
             * Time: 0.002 sec (0 m 0 s)
            */

            if (virusName == null) {
                StringTokenizer st = new StringTokenizer(s);
                String str = null;
                
                for (i=0 ; st.hasMoreTokens() ; i++) {
                    str = st.nextToken();

                    if (i==1) {
                        virusName = str;
                        break;
                    }
                }
            }
        }

        /**
         * PROGRAM EXIT CODES
         * 0      Normal exit.  Nothing found, nothing done.
         * 1      Virus Found
         * 255    Error
         */
        proc.waitFor();
        i = proc.exitValue();
        in.close();
        is.close();

        switch(i) {
        case 0:
            logger.info("clamdscan: clean");
            return VirusScannerResult.CLEAN;
        case 1:
            if (virusName == null) {
                logger.info("clamdscan: infected (unknown)");
                return VirusScannerResult.INFECTED;
            } else {
                logger.info("clamdscan: infected (" + virusName + ")");
                return new VirusScannerResult(false,virusName,false);
            }
        case 2:
        case 255:
            logger.error("clamdscan exit code error: " + i);
            return null;
        default:
            logger.error("Unknown clamdscan exit code: " + i);
            return null;
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
            while (bb.remaining()>0)
                outFile.write(bb);
        }
        outFile.close();

        VirusScannerResult ret = scanFile(fileBuf.getAbsolutePath());
        fileBuf.delete();

        return ret;
    }
}
