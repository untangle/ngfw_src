/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */
package com.untangle.node.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import com.untangle.uvm.LocalUvmContextFactory;

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
        Process proc = LocalUvmContextFactory.context().exec("f-prot " + fileName);
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
