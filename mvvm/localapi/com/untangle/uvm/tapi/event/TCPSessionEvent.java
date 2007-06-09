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
import com.untangle.mvvm.tapi.TCPSession;

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
