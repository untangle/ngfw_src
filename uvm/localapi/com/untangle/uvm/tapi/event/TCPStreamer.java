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

import java.nio.ByteBuffer;

public interface TCPStreamer extends IPStreamer
{
    /**
     * <code>nextChunk</code> should return a ByteBuffer containing
     * the next chunk to be sent.  (Bytes are sent from the buffer's
     * position to its limit).  The buffer should contain a reasonable
     * amount of data for good performance, anywhere from 2K to 16K is
     * usual.  Returns null on EOF.
     *
     * @return a <code>ByteBuffer</code> giving the bytes of the next
     * chunk to send.  Null when EOF
     */
    ByteBuffer nextChunk();
}
