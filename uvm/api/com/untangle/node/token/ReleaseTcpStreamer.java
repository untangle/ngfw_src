/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.TCPStreamer;

/**
 * Flushes a session when unparser receives a <code>Release</code>.
 *
 */
class ReleaseTcpStreamer implements TCPStreamer
{
    private final TCPStreamer streamer;
    private final ReleaseToken release;

    private boolean released = false;

    ReleaseTcpStreamer(TCPStreamer streamer, ReleaseToken release)
    {
        this.streamer = streamer;
        this.release = release;
    }

    public boolean closeWhenDone()
    {
        return true;
    }

    public Object nextChunk()
    {
        if (released) {
            return null;
        } else if (null == streamer) {
            released = true;
            return release;
        } else {
            Object obj = streamer.nextChunk();
            if ( obj == null ) {
                released = true;
                return release;
            } else {
                return obj;
            }
        }
    }
}

