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

package com.untangle.uvm.vnet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.event.TCPStreamer;

/**
 * Streams a File out as a TCP stream.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
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
