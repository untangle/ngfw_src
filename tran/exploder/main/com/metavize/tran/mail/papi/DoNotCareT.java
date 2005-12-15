/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.papi;

import java.nio.ByteBuffer;

import com.metavize.tran.token.Token;

public class DoNotCareT implements Token
{
    private final ByteBuffer zBuf;

    // constructors -----------------------------------------------------------

    public DoNotCareT(ByteBuffer zBuf)
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
}
