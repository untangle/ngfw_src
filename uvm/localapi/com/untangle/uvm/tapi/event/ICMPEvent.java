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
import java.net.*;

/**
 * The class <code>ICMPEvent</code> is for events from incoming ICMP messages that are
 * not associated with a given session.  Currently this means echo-request/echo-reply.
 *
 * @author <a href="mailto:jdi@TOWELIE"></a>
 * @version 1.0
 */
public class ICMPEvent extends MPipeEvent {
    
    private int type;
    private int code;
    
    public ICMPEvent(MPipe mPipe, InetAddress src, InetAddress dst, int type, int code)
    {
        super(mPipe, src);
        this.type   = type;
        this.code   = code;
    }

    public int getType()
    {
        return type;
    }

    public int getCode()
    {
        return code;
    }
}
