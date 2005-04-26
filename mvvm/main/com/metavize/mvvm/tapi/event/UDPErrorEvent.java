/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: ICMPEvent.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.mvvm.tapi.event;

import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.IPPacketHeader;
import com.metavize.mvvm.tapi.UDPSession;
import java.nio.ByteBuffer;

/**
 * The class <code>UDPErrorEvent</code> is for events from incoming ICMP messages that are
 * associated with a given UDP session.
 *
 * @author <a href="mailto:jdi@TOWELIE"></a>
 * @version 1.0
 */
public class UDPErrorEvent extends UDPPacketEvent {
    
    private byte icmpType;
    private byte icmpCode;
    
    public UDPErrorEvent(MPipe mPipe, UDPSession src, ByteBuffer icmpData, IPPacketHeader header, byte icmpType, byte icmpCode)
    {
        super(mPipe, src, icmpData, header);
        this.icmpType   = icmpType;
        this.icmpCode   = icmpCode;
    }

    public byte getErrorType()
    {
        return icmpType;
    }

    public byte getErrorCode()
    {
        return icmpCode;
    }
}
