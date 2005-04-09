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

public class ParseEvent
{
    private ByteBuffer chunk;

    ParseEvent(ByteBuffer chunk)
    {
        this.chunk = chunk;
    }

    public ByteBuffer chunk()
    {
        return chunk;
    }
}
