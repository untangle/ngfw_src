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
import com.metavize.mvvm.tapi.UDPNewSessionRequest;

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
