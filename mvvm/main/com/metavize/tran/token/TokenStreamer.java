/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: TokenStreamer.java,v 1.6 2005/01/30 09:20:30 amread Exp $
 */

package com.metavize.tran.token;

import java.nio.ByteBuffer;

import com.metavize.mvvm.tapi.Pipeline;
import com.metavize.mvvm.tapi.event.TCPStreamer;
import org.apache.log4j.Logger;

public abstract class TokenStreamer implements TCPStreamer
{
    private final Pipeline pipeline;
    private final Logger logger = Logger.getLogger(TokenStreamer.class);

    // constructors -----------------------------------------------------------

    protected TokenStreamer(Pipeline pipeline)
    {
        this.pipeline = pipeline;
    }

    // abstract protected methods ---------------------------------------------

    protected abstract Token nextToken();

    // TCPStreamer methods ----------------------------------------------------

    public ByteBuffer nextChunk()
    {
        logger.debug("streaming next chunk");
        Token tok = nextToken();

        if (null == tok) {
            return null;
        } else {
            // XXX factor out token writing
            ByteBuffer buf = ByteBuffer.allocate(8);
            Long key = pipeline.attach(tok);
            buf.putLong(key);
            buf.flip();
            return buf;
        }
    }
}
