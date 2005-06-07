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

/**
 * A data chunk. This object is only serializable within the same
 * ClassLoader they were serialized in.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class Chunk implements Token
{
    public static final Chunk EMPTY = new Chunk(ByteBuffer.allocate(0));

    private final ByteBuffer data;

    // constructors -----------------------------------------------------------

    public Chunk(ByteBuffer data)
    {
        this.data = data;
    }

    // accessors --------------------------------------------------------------

    public ByteBuffer getData()
    {
        return data;
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        return data.duplicate();
    }
}
