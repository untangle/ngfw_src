/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: UDPStreamer.java,v 1.1 2005/01/21 19:35:23 jdi Exp $
 */

package com.metavize.mvvm.tapi.event;

import java.nio.ByteBuffer;

public interface UDPStreamer extends IPStreamer
{
    /**
     * <code>nextPacket</code> should return a ByteBuffer containing the next packet to
     * be sent.  (Bytes are sent from the buffer's position to its limit).  Packets must
     * be less than the maximum packet size appropriate for the session.  Returns null for
     * "no more packets to send".
     *
     * @return a <code>ByteBuffer</code> giving the bytes of the next chunk to send.  Null when done.
     */
    ByteBuffer nextPacket();
}
