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

/**
 * This token means: drain queued data, send this data out, and
 * release the session.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
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
