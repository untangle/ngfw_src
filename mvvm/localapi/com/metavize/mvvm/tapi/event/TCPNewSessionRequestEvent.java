/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tapi.event;

import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.TCPNewSessionRequest;

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
