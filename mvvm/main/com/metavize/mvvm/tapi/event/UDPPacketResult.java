/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: UDPPacketResult.java,v 1.2 2005/01/04 19:33:51 jdi Exp $
 */

package com.metavize.mvvm.tapi.event;

import com.metavize.mvvm.tapi.MPipeException;
import java.nio.ByteBuffer;


/**
 * Describe class <code>UDPPacketResult</code> here.
 *
 * @author <a href="mailto:jdi@slab.ninthwave.com">John Irwin</a>
 * @version 1.0
 */
public class UDPPacketResult extends IPDataResult
{
    /**
     * Creates a <code>UDPPacketResult</code> to denote that the engine should
     * send the given buffers, one buffer per UDP packet, sent in the given
     * order. One or more of these buffers may be the read-buffer, with the
     * <b>current</b> position and limit used. It may also be some other
     * buffer; again the buffer's current position and limit are used.
     *
     * @param packetsToClient a <code>ByteBuffer[]</code> containing the packets to be sent to the client, with each packet starting at its buffer's position, extending to its buffer's limit
     * @param packetsToServer a <code>ByteBuffer[]</code> containing the packets to be sent to the server, with each packet starting at its buffer's position, extending to its buffer's limit
     */
    public UDPPacketResult(ByteBuffer[] packetsToClient, ByteBuffer[] packetsToServer) {
        super(packetsToClient, packetsToServer, null);
    }

    public ByteBuffer[] packetsToClient() {
        return bufsToClient();
    }

    public ByteBuffer[] packetsToServer() {
        return bufsToServer();
    }
        
}
