/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ProtoFilterImpl.java,v 1.12 2005/02/11 22:47:01 jdi Exp $
 */
package com.metavize.tran.nat;

import java.util.List;
import java.util.LinkedList;

class NatPortList {
    private final List<Integer> portList = new LinkedList<Integer>();
    final int start;
    final int end;
    
    private NatPortList( int start, int end ) {        
        this.start = start;
        this.end   = end;

        for ( int c = start; c <= end ; c++ ) {
            portList.add( c );
        }
    }
    
    static NatPortList makePortList( int start, int end )
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

        return new NatPortList( int protocol, start, end );
    }
    
    int getNextPort()
    {
        return portList.remove( 0 );                
    }

    void addPort( int port )
    {
        if ( start > port || port > end ) {
            throw new IllegalArgumentException( "Invalid port" + port );
        }
        
        portList.add( port );
    }
}
