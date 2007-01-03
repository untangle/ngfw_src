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

package com.untangle.jvector;

public interface SocketQueueListener
{
    public void event( IncomingSocketQueue in, OutgoingSocketQueue out );
    
    public void event( IncomingSocketQueue in );
    public void event( OutgoingSocketQueue out );

    /** This isn't really used because a shutdown on an incoming socket queue will
     * send a crumb */
    public void shutdownEvent( IncomingSocketQueue in );

    /** This is useful for events that must "go backwards" such as a reset read
     * from a TCPSink */
    public void shutdownEvent( OutgoingSocketQueue out );
}
