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

package com.untangle.mvvm.tapi.event;

import com.untangle.mvvm.tapi.MPipe;
import com.untangle.mvvm.tapi.TCPNewSessionRequest;

public class TCPNewSessionRequestEvent extends MPipeEvent {
    
    public TCPNewSessionRequestEvent(MPipe mPipe, TCPNewSessionRequest sessionRequest)
    {
        super(mPipe, sessionRequest);
    }

    public TCPNewSessionRequest sessionRequest()
    {
        return (TCPNewSessionRequest)getSource();
    }
}
