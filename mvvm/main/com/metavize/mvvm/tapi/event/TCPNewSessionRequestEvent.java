/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: TCPNewSessionRequestEvent.java,v 1.4 2005/01/05 01:57:03 jdi Exp $
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
