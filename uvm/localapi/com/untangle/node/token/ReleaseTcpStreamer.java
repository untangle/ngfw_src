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

package com.untangle.node.token;

import java.nio.ByteBuffer;

import com.untangle.uvm.tapi.event.TCPStreamer;

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

