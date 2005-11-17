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

import java.util.List;
import java.util.Collections;
import java.util.LinkedList;

import java.util.Set;
import java.util.HashSet;

import org.apache.log4j.Logger;

import java.util.Random;

class PortList {
    private final List<Integer> portList = new LinkedList<Integer>();

    private final Set<Integer> usedPortSet = new HashSet<Integer>();

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
        if ( start < 0 || start > 65535 ) {
            throw new IllegalArgumentException( "Invalid start port: " + start );
        }
        
        if ( end < 0 || end > 65535 ) {
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
        
        if ( !usedPortSet.add( port )) {
            logger.error( "Reusing port that is on the used port list: " + port );
        }
        
        return port;
    }

    synchronized void releasePort( int port )
    {
        if ( start > port || port > end ) {
            throw new IllegalArgumentException( "Invalid port: " + port );
        }

        if ( !usedPortSet.remove( port )) {
            logger.error( "Removing port that is not on the used list: " + port );
        }
        
        portList.add( port );
    }
}
