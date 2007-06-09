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

package com.untangle.uvm.tapi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.untangle.uvm.tapi.event.TCPStreamer;
import org.apache.log4j.Logger;

public class FileStreamer implements TCPStreamer
{
    private final FileChannel channel;
    private final int chunkSize;
    private final boolean closeWhenDone;
    private final Logger logger = Logger.getLogger(FileStreamer.class);

    // constructors -----------------------------------------------------------

    public FileStreamer(FileChannel channel, boolean closeWhenDone,
                        int chunkSize)
    {
        logger.debug("new file streamer");
        this.channel = channel;
        this.closeWhenDone = closeWhenDone;
        this.chunkSize = chunkSize;
    }

    public FileStreamer(FileChannel channel, boolean closeWhenDone)
    {
        logger.debug("new file streamer");
        this.channel = channel;
        this.closeWhenDone = closeWhenDone;
        this.chunkSize = 16384;
    }

    // TCPStreamer methods ----------------------------------------------------

    public boolean closeWhenDone()
    {
        return closeWhenDone;
    }

    public ByteBuffer nextChunk()
    {
        logger.debug("streaming bytes");
        try {
            ByteBuffer buf = ByteBuffer.allocate(chunkSize);

            // logger.debug( "nextChunk -  before position: " + channel.position());

            if (0 > channel.read(buf)) {
                return null; /* done */
            } else {
                buf.flip();
                // logger.debug("nextChunk - returning buffer: " + buf + "/ at position:" + channel.position());

                return buf;
            }
        } catch (IOException exn) {
            logger.debug("could not stream file", exn);
            return null; // XXX should I rst or something?
        }
    }
}
