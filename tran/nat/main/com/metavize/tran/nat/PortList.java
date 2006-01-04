/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.nat;

import java.util.BitSet;

import org.apache.log4j.Logger;

class PortList {
    private static final int MAX_PORTS = 65535;

    private final BitSet usedPortSet = new BitSet(MAX_PORTS);

    private final Logger logger = Logger.getLogger( PortList.class );

    private final int start;
    private final int range;

    private int nextPort;

    private PortList( int start, int end ) {
        this.start = this.nextPort = start;
        this.range = end - start;
    }

    static PortList makePortList( int start, int end )
    {
        if ( start < 0 || start > MAX_PORTS ) {
            throw new IllegalArgumentException( "Invalid start port: " + start );
        }

        if ( end < 0 || end > MAX_PORTS ) {
            throw new IllegalArgumentException( "Invalid end port: " + end );
        }

        if ( start > end ) {
            throw new IllegalArgumentException( "Start(" + start + ") < end(" + end + ")" );
        }

        return new PortList( start, end );
    }

    synchronized int getNextPort()
    {
        for (int i = 0; i < range; i++) {
            nextPort = (nextPort + i) % range;
            if (!usedPortSet.get(nextPort)) {
                usedPortSet.set(nextPort);
                return nextPort++;
            }
        }

        // XXX we need to do something here!!!
        logger.error("Reusing port: " + nextPort );
        return nextPort++;
    }

    synchronized void releasePort( int port )
    {
        if ( start > port || port > start + range ) {
            throw new IllegalArgumentException( "Invalid port: " + port );
        }

        if (!usedPortSet.get(port)) {
            // XXX we need to do something here
            logger.error( "Removing port that is not on the used list: " + port );
        }
        usedPortSet.clear(port);
    }
}
