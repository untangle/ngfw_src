/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ParseEvent.java,v 1.1.1.1 2004/12/01 23:32:23 amread Exp $
 */

package com.metavize.tran.token;

import java.nio.ByteBuffer;

public class ParseEvent
{
    private ParseSession parseSession;
    private ByteBuffer chunk;

    ParseEvent(ParseSession parseSession, ByteBuffer chunk)
    {
        this.parseSession = parseSession;
        this.chunk = chunk;
    }

    public ParseSession parseSession()
    {
        return parseSession;
    }

    public ByteBuffer chunk()
    {
        return chunk;
    }
}
