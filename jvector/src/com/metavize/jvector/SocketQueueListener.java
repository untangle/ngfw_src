/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SocketQueueListener.java,v 1.3 2005/01/20 22:20:54 rbscott Exp $
 */

package com.metavize.jvector;

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
