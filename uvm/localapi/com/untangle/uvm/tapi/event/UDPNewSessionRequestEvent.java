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
import com.untangle.uvm.tapi.UDPNewSessionRequest;

public class UDPNewSessionRequestEvent extends MPipeEvent {
    
    public UDPNewSessionRequestEvent(MPipe mPipe, UDPNewSessionRequest sessionRequest)
    {
        super(mPipe, sessionRequest);
    }

    public UDPNewSessionRequest sessionRequest()
    {
        return (UDPNewSessionRequest)getSource();
    }
}
