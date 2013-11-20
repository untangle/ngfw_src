/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.event.TCPStreamer;

/**
 * Flushes a session when unparser receives a <code>Release</code>.
 *
 */
class ReleaseTcpStreamer implements TCPStreamer
{
    private final TCPStreamer streamer;
    private final Release release;

    private boolean released = false;

    ReleaseTcpStreamer(TCPStreamer streamer, Release release)
    {
        this.streamer = streamer;
        this.release = release;
    }

    public boolean closeWhenDone()
    {
        return true;
    }

    public ByteBuffer nextChunk()
    {
        if (released) {
            return null;
        } else if (null == streamer) {
            released = true;
            return release.getBytes();
        } else {
            ByteBuffer bb = streamer.nextChunk();
            if (null == bb) {
                released = true;
                return release.getBytes();
            } else {
                return bb;
            }
        }
    }
}

