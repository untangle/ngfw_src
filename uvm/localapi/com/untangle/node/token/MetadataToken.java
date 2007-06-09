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

public class MetadataToken implements Token
{
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    // Token methods ----------------------------------------------------------

    public final ByteBuffer getBytes()
    {
        return EMPTY_BUFFER;
    }

    public int getEstimatedSize()
    {
        return 0;
    }
}
