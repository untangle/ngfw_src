/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ClamScanner.java,v 1.1 2005/03/11 03:34:57 cng Exp $
 */
package com.metavize.tran.email;

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

public class ClamScanner implements VirusScanner
{
    private static final Logger logger = Logger.getLogger(ClamScanner.class.getName());

    public ClamScanner() {}

    public VirusScannerResult scanFile (String pathName)
        throws IOException, InterruptedException
    {
        /* Clam clamscan can process special files (archives/zip/etc) and
         * by default, it handles these special files
         */
        Process proc = Runtime.getRuntime().exec("nice -n 19 clamscan " + pathName);
        InputStream is  = proc.getInputStream();
        OutputStream os = proc.getOutputStream();

        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        os.close();

        String virusName = null;
        String s;
        int i;

        /**
         * Drain clamscan output
         */
        while ((s = in.readLine()) != null) {
            /**
             * clamscan output
             * "/home/xxx/zzz/q347558.exe.2: Worm.Gibe.F FOUND"
             * This returns the 2nd word
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
         * 0 : No virus found.
         * 1 : Virus(es) found.
         * 40: Unknown option passed.
         * 50: Database initialization error.
         * 52: Not supported file type.
         * 53: Can't open directory.
         * 54: Can't open file. (ofm)
         * 55: Error reading file. (ofm)
         * 56: Can't stat input file / directory.
         * 57: Can't get absolute path name of current working directory.
         * 58: I/O error, please check your filesystem.
         * 59: Can't get information about current user from /etc/passwd.
         * 60: Can't get information about user 'clamav' (default name) from /etc/passwd.
         * 61: Can't fork.
         * 63: Can't create temporary files/directories (check permissions).
         * 64: Can't write to temporary directory (please specify another one).
         * 70: Can't allocate and clear memory (calloc).
         * 71: Can't allocate memory (malloc).
         */
        switch(i) {
        case 0:
            return VirusScannerResult.CLEAN;
        case 1:
            if (null == virusName)
                return VirusScannerResult.INFECTED;
            else
                return new VirusScannerResult(false,virusName,false);
        default:
            logger.error("clamscan exit code: " + i);
            return null;
        }
    }

    public VirusScannerResult scanBufs(List bufs)
        throws IOException, InterruptedException
    {
        File fileBuf = File.createTempFile("clamscan-cache",null);
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
