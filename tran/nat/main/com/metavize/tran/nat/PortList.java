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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

class PortList {
    private static final int MAX_PORTS = 65535;

    private final List<Integer> portList = new LinkedList<Integer>();

    private final BitSet usedPortSet = new BitSet(MAX_PORTS);

    private final Logger logger = Logger.getLogger( PortList.class );

    final int start;
    final int end;

    private PortList( int start, int end ) {
        this.start = start;
        this.end   = end;

        for ( int c = start; c <= end ; c++ ) {
            portList.add( c );
        }

        /* Randomize the port list */
        Collections.shuffle( portList, new Random());
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
        int port = portList.remove( 0 );

        if (usedPortSet.get(port)) {
            logger.error( "Reusing port that is on the used port list: " + port );
        }
        usedPortSet.set(port);

        return port;
    }

    synchronized void releasePort( int port )
    {
        if ( start > port || port > end ) {
            throw new IllegalArgumentException( "Invalid port: " + port );
        }

        if (usedPortSet.get(port)) {
            logger.error( "Removing port that is not on the used list: " + port );
        }
        usedPortSet.set(port);

        portList.add( port );
    }
}
