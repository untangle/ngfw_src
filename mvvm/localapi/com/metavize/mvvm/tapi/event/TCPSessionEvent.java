/*
 * Copyright (c) 2003 Metavize Inc.
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
import com.metavize.mvvm.tapi.TCPSession;

public class TCPSessionEvent extends IPSessionEvent {
    
    public TCPSessionEvent(MPipe mPipe, TCPSession session)
    {
        super(mPipe, session);
    }

    public TCPSession session()
    {
        return (TCPSession)getSource();
    }
}
