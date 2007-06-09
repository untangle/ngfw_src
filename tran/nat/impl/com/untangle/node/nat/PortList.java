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
package com.untangle.tran.nat;

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
        int d = nextPort - start;

        for (int i = 0; i < range; i++) {
            d = (d + i) % range;
            nextPort = start + d;
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

    public static void main(String[] args)
    {
        PortList pl = new PortList(100, 200);

        for (int i = 0; i < 200; i++) {
            int p = pl.getNextPort();
            pl.releasePort(p);
        }
    }
}
