/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SinkEndpointListener.java,v 1.1 2005/01/29 01:34:42 rbscott Exp $
 */

package com.metavize.jvector;

public interface SinkEndpointListener
{
    /**
     * An event that occurs each time the endpoint transmits a part
     * of a crumb.
     * @param sink - The sink that triggered the event.
     * @param numBytes - The number of bytes transmitted.
     */
    void dataEvent( Sink sink, int numBytes );


    /**
     * An event that occurs when the source or sink shuts down
     * @param sink - The sink that triggered the event.
     */
    void shutdownEvent( Sink sink );
}
