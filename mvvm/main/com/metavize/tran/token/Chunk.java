/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Chunk.java,v 1.4 2005/01/27 09:53:35 amread Exp $
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
    private ByteBuffer data;

    public Chunk(ByteBuffer data)
    {
        this.data = data;
    }

    public ByteBuffer getData()
    {
        return data;
    }

    public void setData(ByteBuffer data)
    {
        this.data = data;
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        return data.duplicate();
    }
}
