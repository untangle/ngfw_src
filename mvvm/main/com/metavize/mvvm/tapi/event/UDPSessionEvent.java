/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: UDPSessionEvent.java,v 1.1 2004/12/18 00:44:23 jdi Exp $
 */

package com.metavize.mvvm.tapi.event;

import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.UDPSession;

public class UDPSessionEvent extends IPSessionEvent {
    
    public UDPSessionEvent(MPipe mPipe, UDPSession session)
    {
        super(mPipe, session);
    }

    public UDPSession session()
    {
        return (UDPSession)getSource();
    }
}
