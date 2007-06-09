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

package com.untangle.mvvm.argon;

public interface NewSessionEventListener 
{
    /**
     * A new UDP session event event.  This function converts a request into a session.</p>
     * 
     * @param request - A UDP Session request.
     */
    public UDPSession newSession( UDPNewSessionRequest request, boolean isInbound );

    /**
     * A new TCP session event event.  This function converts a request into a session.</p>
     * 
     * @param request - A TCP Session request.
     */
    public TCPSession newSession( TCPNewSessionRequest request, boolean isInbound );
}
