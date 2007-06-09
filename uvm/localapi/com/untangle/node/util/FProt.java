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
package com.untangle.tran.util;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

import com.untangle.mvvm.MvvmContextFactory;

public class FProt {

    public static final int VIRUS_FREE = 0;
    public static final int VIRUS_FOUND = 1;
    public static final int VIRUS_REMOVED = 2;
    public static final int VIRUS_ERROR = 3;

    private static final int CHUNK_SIZE = 8192;

    private FProt()
    {

    }

    public static int scanFile (String fileName) throws IOException,InterruptedException
    {
        byte[] outbuf = new byte[CHUNK_SIZE];
        Process proc = MvvmContextFactory.context().exec("f-prot " + fileName);
        InputStream is  = proc.getInputStream();
        OutputStream os = proc.getOutputStream();
        int i;

        os.close();

        /**
         * Drain f-prots output
         * XXX contains useful info
         */
        while ((i = is.read(outbuf,0,CHUNK_SIZE)) > 0);

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
        is.close();

        switch(i) {
        case 0:
            return FProt.VIRUS_FREE;
        case 3:
        case 8:
            return FProt.VIRUS_FOUND;
        case 6:
            return FProt.VIRUS_REMOVED;
        case 1:
        case 2:
        case 4:
        case 5:
        case 7:
            System.err.println("F-prot exit code error: " + i);
            return FProt.VIRUS_ERROR;
        default:
            System.err.println("Unknown f-prot exit code: " + i);
            return FProt.VIRUS_ERROR;
        }
    }

    public static int scanBufs (ArrayList bufs) throws IOException,InterruptedException
    {
        File fileBuf = File.createTempFile("f-prot-cache",null);
        FileChannel outFile = (new FileOutputStream(fileBuf)).getChannel();
        int ret,i;

        for (i = 0; i < bufs.size(); i++) {
            ByteBuffer bb = (ByteBuffer)bufs.get(i);
            bb.flip();
            while (bb.remaining()>0)
                outFile.write(bb);
        }
        outFile.close();

        ret = scanFile(fileBuf.getAbsolutePath());
        fileBuf.delete();

        return ret;
    }

}
