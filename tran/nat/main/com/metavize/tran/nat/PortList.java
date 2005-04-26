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
import java.util.LinkedList;

class PortList {
    private final List<Integer> portList = new LinkedList<Integer>();
    final int start;
    final int end;
    
    private PortList( int start, int end ) {        
        this.start = start;
        this.end   = end;

        for ( int c = start; c <= end ; c++ ) {
            portList.add( c );
        }
    }
    
    static PortList makePortList( int start, int end )
    {
        if ( start < 0 || start > 65535 ) {
            throw new IllegalArgumentException( "Invalid start port" + start );
        }
        
        if ( end < 0 || end > 65535 ) {
            throw new IllegalArgumentException( "Invalid end port" + end );
        }
        
        if ( start > end ) {
            throw new IllegalArgumentException( "Start(" + start + ") < end(" + end + ")" );
        }

        return new PortList( start, end );
    }
    
    boolean isInPortRange( int port )
    {
        if ( start <= port && port <=  end ) {
            return true;
        }
        
        return false;
    }
    
    synchronized int getNextPort()
    {
        return portList.remove( 0 );                
    }

    synchronized void releasePort( int port )
    {
        if ( start > port || port > end ) {
            throw new IllegalArgumentException( "Invalid port" + port );
        }
        
        portList.add( port );
    }
}
