/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: IPSessionEvent.java,v 1.1 2004/12/18 00:44:22 jdi Exp $
 */

package com.metavize.mvvm.tapi.event;

import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.IPSession;

public class IPSessionEvent extends SessionEvent {

    public IPSessionEvent(MPipe mPipe, IPSession session)
    {
        super(mPipe, session);
    }

    public IPSession ipsession()
    {
        return (IPSession)getSource();
    }

}
