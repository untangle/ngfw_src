/*
 * Copyright (c) 2005, 2006 Metavize Inc.
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
 * This token means: drain queued data, send this data out, and
 * release the session.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class Release implements Token
{
    public static final Release EMPTY = new Release(ByteBuffer.allocate(0));

    private final ByteBuffer data;

    // constructors -----------------------------------------------------------

    public Release(ByteBuffer data)
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

    public int getEstimatedSize()
    {
        return 0;
    }
}
