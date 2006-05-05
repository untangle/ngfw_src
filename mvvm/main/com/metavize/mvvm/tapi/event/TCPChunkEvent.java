/*
 * Copyright (c) 2003, 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tapi.event;

import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.TCPSession;
import java.nio.ByteBuffer;

public class TCPChunkEvent extends TCPSessionEvent
    implements IPDataEvent
{
    private ByteBuffer readBuffer;

    public TCPChunkEvent(MPipe mPipe, TCPSession session,
                         ByteBuffer readBuffer)
    {
        super(mPipe, session);
        this.readBuffer = readBuffer;
    }

    public ByteBuffer chunk()
    {
        return readBuffer;
    }

    public ByteBuffer data()
    {
        return readBuffer;
    }
}
