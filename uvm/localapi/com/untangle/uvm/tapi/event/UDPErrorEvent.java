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
import java.net.InetAddress;

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
    private InetAddress icmpSource;
    
    public UDPErrorEvent(MPipe mPipe, UDPSession src, ByteBuffer icmpData, IPPacketHeader header, byte icmpType, byte icmpCode, InetAddress icmpSource)
    {
        super(mPipe, src, icmpData, header);
        this.icmpType   = icmpType;
        this.icmpCode   = icmpCode;
        this.icmpSource = icmpSource;
    }

    public byte getErrorType()
    {
        return icmpType;
    }

    public byte getErrorCode()
    {
        return icmpCode;
    }
    
    public InetAddress getErrorSource()
    {
        return icmpSource;
    }
}
