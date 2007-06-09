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

package com.untangle.tran.token;

import java.nio.ByteBuffer;

import com.untangle.mvvm.tapi.Pipeline;
import com.untangle.mvvm.tapi.event.TCPStreamer;
import org.apache.log4j.Logger;

public class TokenStreamerAdaptor implements TCPStreamer
{
    private final Logger logger = Logger.getLogger(getClass());

    private final Pipeline pipeline;
    private final TokenStreamer streamer;

    public TokenStreamerAdaptor(Pipeline pipeline, TokenStreamer streamer)
    {
        this.pipeline = pipeline;
        this.streamer = streamer;
    }

    // TCPStreamer methods ----------------------------------------------------

    public boolean closeWhenDone()
    {
        return streamer.closeWhenDone();
    }

    public ByteBuffer nextChunk()
    {
        logger.debug("streaming next chunk");
        Token tok = streamer.nextToken();

        if (null == tok) {
            return null;
        } else {
            // XXX factor out token writing
            ByteBuffer buf = ByteBuffer.allocate(8);
            Long key = pipeline.attach(tok);
            logger.debug("streaming tok: " + tok + " with key: " + key);
            buf.putLong(key);
            buf.flip();
            return buf;
        }
    }
}
