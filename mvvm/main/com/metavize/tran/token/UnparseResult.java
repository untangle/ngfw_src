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

public class UnparseResult
{
    private static final ByteBuffer[] BYTE_BUFFER_PROTO = new ByteBuffer[0];
    // XXX make List<ByteBuffer> when no XDoclet
    private final ByteBuffer[] result;

    // XXX make List<ByteBuffer> when no XDoclet
    public UnparseResult(ByteBuffer[] result)
    {
        this.result = null == result ? BYTE_BUFFER_PROTO : result;
    }

    public UnparseResult(ByteBuffer result)
    {
        this.result = new ByteBuffer[] { result };
    }

    public UnparseResult()
    {
        this.result = BYTE_BUFFER_PROTO;
    }

    public ByteBuffer[] result()
    {
        return result;
    }
}
