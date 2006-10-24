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

package com.metavize.tran.mail.papi.pop;

import java.nio.ByteBuffer;

import com.metavize.tran.token.Token;

public class PopReplyMore implements Token
{
    private final ByteBuffer zBuf;

    // constructors -----------------------------------------------------------

    public PopReplyMore(ByteBuffer zBuf)
    {
        this.zBuf = zBuf;
    }

    // static factories -------------------------------------------------------

    // accessors --------------------------------------------------------------

    public ByteBuffer getBuf()
    {
        return zBuf;
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        return zBuf.duplicate();
    }

    public int getEstimatedSize()
    {
        return zBuf.remaining();
    }
}
