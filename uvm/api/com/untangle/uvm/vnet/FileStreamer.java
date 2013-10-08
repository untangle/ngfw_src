/**
 * $Id: FileStreamer.java 34108 2013-03-04 19:34:24Z dmorris $
 */
package com.untangle.uvm.vnet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.event.TCPStreamer;

/**
 * Streams a File out as a TCP stream.
 */
public class FileStreamer implements TCPStreamer
{
    private final FileChannel channel;
    private final int chunkSize;
    private final boolean closeWhenDone;
    private final Logger logger = Logger.getLogger(FileStreamer.class);

    // constructors -----------------------------------------------------------

    public FileStreamer( FileChannel channel, boolean closeWhenDone, int chunkSize )
    {
        logger.debug("new file streamer");
        this.channel = channel;
        this.closeWhenDone = closeWhenDone;
        this.chunkSize = chunkSize;
    }

    public FileStreamer( FileChannel channel, boolean closeWhenDone )
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
