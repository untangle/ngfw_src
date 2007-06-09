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

package com.untangle.uvm.tapi.event;

import com.untangle.uvm.tapi.MPipe;
import com.untangle.uvm.tapi.IPPacketHeader;
import com.untangle.uvm.tapi.UDPSession;
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
