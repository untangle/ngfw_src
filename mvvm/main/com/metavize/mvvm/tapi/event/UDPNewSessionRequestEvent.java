/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: UDPNewSessionRequestEvent.java,v 1.4 2005/01/05 01:57:03 jdi Exp $
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
