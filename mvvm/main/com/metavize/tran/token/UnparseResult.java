/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.token;

import java.nio.ByteBuffer;

import com.metavize.mvvm.tapi.event.TCPStreamer;

public class UnparseResult
{
    private static final ByteBuffer[] BYTE_BUFFER_PROTO = new ByteBuffer[0];
    // XXX make List<ByteBuffer> when no XDoclet
    private final ByteBuffer[] result;
    private final TCPStreamer tcpStreamer;

    // XXX make List<ByteBuffer> when no XDoclet
    public UnparseResult(ByteBuffer[] result)
    {
        this.result = null == result ? BYTE_BUFFER_PROTO : result;
        this.tcpStreamer = null;
    }

    public UnparseResult(ByteBuffer result)
    {
        this.result = new ByteBuffer[] { result };
        this.tcpStreamer = null;
    }

    public UnparseResult(TCPStreamer tcpStreamer)
    {
        this.result = BYTE_BUFFER_PROTO;
        this.tcpStreamer = tcpStreamer;
    }

    public UnparseResult()
    {
        this.result = BYTE_BUFFER_PROTO;
        this.tcpStreamer = null;
    }

    public ByteBuffer[] result()
    {
        return result;
    }

    public TCPStreamer getTcpStreamer()
    {
        return tcpStreamer;
    }

    public boolean isStreamer()
    {
        return null != tcpStreamer;
    }
}
