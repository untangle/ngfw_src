/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SourceEndpointListener.java,v 1.1 2005/01/29 01:34:42 rbscott Exp $
 */

package com.metavize.jvector;

public interface SourceEndpointListener
{
    /**
     * An event that occurs each time the endpoint receives data.
     * @param source - The source that triggered the event.
     * @param numBytes - The number of bytes transmitted.
     */
    void dataEvent( Source source, int numBytes );


    /**
     * An event that occurs when the source or sink shuts down
     * @param source - The source that triggered the event.
     */
    void shutdownEvent( Source source );
}
