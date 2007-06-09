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

package com.untangle.mvvm.tapi.event;

import com.untangle.mvvm.tapi.MPipe;
import com.untangle.mvvm.tapi.TCPSession;
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
