/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tapi.event;

import com.metavize.mvvm.tapi.MPipe;
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
