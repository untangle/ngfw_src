/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: UDPPacketEvent.java,v 1.1 2004/12/18 00:44:23 jdi Exp $
 */

package com.metavize.mvvm.tapi.event;

import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.UDPSession;
import java.nio.ByteBuffer;

public class UDPPacketEvent extends UDPSessionEvent
    implements IPDataEvent
{
    private ByteBuffer packetBuffer;

    public UDPPacketEvent(MPipe mPipe, UDPSession session,
                          ByteBuffer packetBuffer)
    {
        super(mPipe, session);
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
}
