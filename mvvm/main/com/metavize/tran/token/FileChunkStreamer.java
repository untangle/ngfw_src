/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.token;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.metavize.mvvm.tapi.Pipeline;
import org.apache.log4j.Logger;

/**
 * Streams a file out as chunks.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class FileChunkStreamer extends TokenStreamer
{
    private final Logger logger = Logger.getLogger(FileChunkStreamer.class);

    private final File file;
    private final FileChannel channel;
    private final boolean closeWhenDone;
    private final int chunkSize;

    private boolean sentEnd = false;

    // constructors -----------------------------------------------------------

    public FileChunkStreamer(Pipeline pipeline, File file, FileChannel channel,
                             boolean closeWhenDone, int chunkSize)
    {
        super(pipeline);
        this.file = file;
        this.channel = channel;
        this.closeWhenDone = closeWhenDone;
        this.chunkSize = chunkSize;
    }

    public FileChunkStreamer(Pipeline pipeline, File file, FileChannel channel,
                             boolean closeWhenDone)
    {
        super(pipeline);
        this.file = file;
        this.channel = channel;
        this.closeWhenDone = closeWhenDone;
        this.chunkSize = 16384;
    }

    // TCPStreamer methods ----------------------------------------------------

    public boolean closeWhenDone()
    {
        return closeWhenDone;
    }

    // TokenStreamer methods --------------------------------------------------

    protected Token nextToken()
    {
        logger.debug("streaming token");
        try {
            ByteBuffer buf = ByteBuffer.allocate(chunkSize);

            if (sentEnd) {
                return null; /* done */
            } else if (0 > channel.read(buf)) {
                channel.close();
                file.delete();
                sentEnd = true;
                return EndMarker.MARKER;
            } else {
                buf.flip();
                return new Chunk(buf);
            }
        } catch (IOException exn) {
            logger.debug("could not stream file", exn);
            return null; // XXX i need to be able to rst or something
        }
    }
}
