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

public class UnparseResult
{
    public static final UnparseResult NONE;
    private static final ByteBuffer[] BYTE_BUFFER_PROTO;

    static {
        BYTE_BUFFER_PROTO = new ByteBuffer[0];
        NONE = new UnparseResult();
    }

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

    private UnparseResult()
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
