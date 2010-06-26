/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.mail.impl.pop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.ByteBufferByteStuffer;
import com.untangle.uvm.vnet.event.TCPStreamer;

public class PopStreamer implements TCPStreamer
{
    /* constants */
    private final Logger zLog = Logger.getLogger(getClass());
    private static final int DATA_SZ = 8192;
    private static final int READ_FAIL = -100;

    /* class variables */

    /* instance variables */
    private File zFile;
    private FileChannel zFileChannel;
    private ByteBuffer zLeadBuf;
    private int iFileSz;
    private int iDataSz;

    /* constructors */
    private PopStreamer() {}

    /* public methods */
    public ByteBuffer nextChunk()
    {
        if (null != zLeadBuf) {
            ByteBuffer zTmpBuf = zLeadBuf;
            zLeadBuf = null;
            //zLog.debug("stream (lead): " + zTmpBuf);
            return zTmpBuf;
        }

        ByteBuffer zReadBuf = ByteBuffer.allocate(iDataSz);

        if (0 < readFile(zFileChannel, zReadBuf)) {
            zReadBuf.flip();
            //zLog.debug("stream: " + zReadBuf);
            return zReadBuf;
        }

        //zLog.debug("stream: empty");
        return null;
    }

    public boolean closeWhenDone()
    {
        zLog.debug("stream: done (close = false)");

        closeChannel(zFileChannel);
        zFile.delete();
        zFile = null;
        zFileChannel = null;
        return false; /* never close write socket queue */
    }

    public static PopStreamer stuffFile(File zOrgFile, File zNewFile, ByteBufferByteStuffer zByteStuffer, boolean bIsComplete)
    {
        Logger logger = Logger.getLogger(PopStreamer.class);

        FileChannel zReadChannel;
        try {
            zReadChannel = new FileInputStream(zOrgFile).getChannel();
        } catch (FileNotFoundException exn) {
            logger.debug("cannot open input file channel for byte unstuffed message file: " + exn);
            return null;
        }

        FileChannel zWriteChannel;
        try {
            zWriteChannel = new FileOutputStream(zNewFile).getChannel();
        } catch (IOException exn) {
            logger.debug("cannot open output file channel for byte stuffed message file: " + exn);
            closeChannel(zReadChannel);
            return null;
        }

        long lOrgFileSz = zOrgFile.length();
        logger.debug("byte unstuffed message file size: " + lOrgFileSz);
        int iDataSz = (int) ((DATA_SZ < lOrgFileSz) ? DATA_SZ : lOrgFileSz);
        ByteBuffer zReadBuf = ByteBuffer.allocate(iDataSz);
        ByteBuffer zWriteBuf = ByteBuffer.allocate(iDataSz);
        int iNewFileSz = 0;

        int iReadCt = 0;

        while (0 < (iReadCt = readFile(zReadChannel, zReadBuf))) {
            zReadBuf.flip();
            zByteStuffer.transfer(zReadBuf, zWriteBuf);
            iNewFileSz += zWriteBuf.remaining();

            if (false == writeFile(zWriteChannel, zWriteBuf)) {
                closeChannel(zReadChannel);
                closeChannel(zWriteChannel);
                return null;
            }

            zReadBuf.clear();
            zWriteBuf.clear();
        }

        if (READ_FAIL == iReadCt) {
            closeChannel(zReadChannel);
            closeChannel(zWriteChannel);
            return null;
        }

        zWriteBuf = zByteStuffer.getLast(bIsComplete);
        iNewFileSz += zWriteBuf.remaining();
        if (false == writeFile(zWriteChannel, zWriteBuf)) {
            closeChannel(zReadChannel);
            closeChannel(zWriteChannel);
            return null;
        }

        closeChannel(zReadChannel);
        closeChannel(zWriteChannel);
        logger.debug("byte stuffed message file size: " + iNewFileSz + ", " + zNewFile.length());

        /* ready to build PopStreamer */
        PopStreamer zPopStreamer = new PopStreamer();
        zPopStreamer.zFile = zNewFile;
        try {
            zPopStreamer.zFileChannel = new FileInputStream(zNewFile).getChannel();
        } catch (FileNotFoundException exn) {
            logger.debug("cannot open input file channel for byte stuffed message file: " + exn);
            return null;
        }
        zPopStreamer.iDataSz = ((DATA_SZ < iNewFileSz) ? DATA_SZ : iNewFileSz);
        zPopStreamer.iFileSz = iNewFileSz;

        return zPopStreamer;
    }

    public void prepend(ByteBuffer zBuf)
    {
        zLeadBuf = zBuf;
        return;
    }

    public int getSize()
    {
        return iFileSz;
    }

    /* private methods */
    private static int readFile(FileChannel zReadChannel, ByteBuffer zReadBuf)
    {
        try {
            return zReadChannel.read(zReadBuf);
        } catch (IOException exn) {
            Logger logger = Logger.getLogger(PopStreamer.class);
            logger.warn("cannot read from message file: " + exn);
            return READ_FAIL;
        }
    }

    private static boolean writeFile(FileChannel zWriteChannel, ByteBuffer zWriteBuf)
    {
        try {
            for (; true == zWriteBuf.hasRemaining(); ) {
                zWriteChannel.write(zWriteBuf);
            }

            return true;
        } catch (IOException exn) {
            Logger logger = Logger.getLogger(PopStreamer.class);
            logger.warn("cannot write data to message file: " + exn);
            return false;
        }
    }

    private static void closeChannel(FileChannel zChannel)
    {
        try {
            zChannel.close();
        } catch (IOException exn) {
            Logger logger = Logger.getLogger(PopStreamer.class);
            logger.warn("cannot close file channel: " + exn);
        }

        return;
    }
}
