/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: NewSessionEventListener.java,v 1.3 2005/01/06 20:53:44 rbscott Exp $
 */

package com.metavize.mvvm.argon;

public interface NewSessionEventListener 
{
    /**
     * A new UDP session event event.  This function converts a request into a session.</p>
     * 
     * @param request - A UDP Session request.
     */
    public UDPSession newSession( UDPNewSessionRequest request );

    /**
     * A new TCP session event event.  This function converts a request into a session.</p>
     * 
     * @param request - A TCP Session request.
     */
    public TCPSession newSession( TCPNewSessionRequest request );
}
