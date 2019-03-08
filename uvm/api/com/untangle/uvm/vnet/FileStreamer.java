/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.TCPStreamer;

/**
 * Streams a File out as a TCP stream.
 */
public class FileStreamer implements TCPStreamer
{
    private final FileChannel channel;
    private final int chunkSize;
    private final boolean closeWhenDone;
    private final Logger logger = Logger.getLogger(FileStreamer.class);

    /**
     * FileStreamer constructor
     * @param channel
     * @param closeWhenDone
     * @param chunkSize
     */
    public FileStreamer( FileChannel channel, boolean closeWhenDone, int chunkSize )
    {
        logger.debug("new file streamer");
        this.channel = channel;
        this.closeWhenDone = closeWhenDone;
        this.chunkSize = chunkSize;
    }

    /**
     * FileStreamer constructor
     * @param channel
     * @param closeWhenDone
     */
    public FileStreamer( FileChannel channel, boolean closeWhenDone )
    {
        logger.debug("new file streamer");
        this.channel = channel;
        this.closeWhenDone = closeWhenDone;
        this.chunkSize = 16384;
    }

    /**
     * closeWhenDone true if should be closed when done
     * @return bool
     */
    public boolean closeWhenDone()
    {
        return closeWhenDone;
    }

    /**
     * nextChunk returns the next chunk/token
     * @return ByteBuffer
     */
    public ByteBuffer nextChunk()
    {
        logger.debug("streaming bytes");
        try {
            ByteBuffer buf = ByteBuffer.allocate(chunkSize);

            // logger.debug( "nextChunk -  before position: " + channel.position());
            if ( channel.read(buf) < 0 ) {
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
