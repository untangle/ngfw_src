/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.token;

import java.nio.ByteBuffer;

import com.metavize.mvvm.tapi.event.TCPStreamer;

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

