/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tapi.event;

import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.IPPacketHeader;
import com.metavize.mvvm.tapi.UDPSession;
import java.nio.ByteBuffer;

public class UDPPacketEvent extends UDPSessionEvent
    implements IPDataEvent
{
    private ByteBuffer packetBuffer;
    private IPPacketHeader header;

    public UDPPacketEvent(MPipe mPipe, UDPSession session,
                          ByteBuffer packetBuffer,
                          IPPacketHeader header)
    {
        super(mPipe, session);
        this.header = header;
        this.packetBuffer = packetBuffer;
    }

    public ByteBuffer packet()
    {
        return packetBuffer;
    }

    public ByteBuffer data()
    {
        return packetBuffer;
    }

    public IPPacketHeader header()
    {
        return header;
    }
}
