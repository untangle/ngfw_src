/*
 * Copyright (c) 2003, 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: TCPChunkEvent.java,v 1.1 2004/12/18 00:44:22 jdi Exp $
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
