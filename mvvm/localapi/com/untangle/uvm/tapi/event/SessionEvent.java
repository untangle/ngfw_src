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
import com.untangle.mvvm.tapi.Session;

/**
 * Describe class <code>SessionEvent</code> here.
 *
 * For all session events, the source is the session.
 *
 * @author <a href="mailto:jdi@bebe">jdi</a>
 * @version 1.0
 */
public abstract class SessionEvent extends MPipeEvent {
    
    protected SessionEvent(MPipe mPipe, Session session)
    {
        super(mPipe, session);
    }

    // We could define getSession here, but it's more convenient to define it
    // only at the leaves, so that you don't need getSession, getIPSession,
    // getTCPSession, etc.
}
