/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: InterfaceSet.java,v 1.5 2004/12/20 19:54:06 rbscott Exp $
 */

package com.metavize.jnetcap;

public class InterfaceSet {
    private int data;
    
    InterfaceSet () {
        this.data = clear();
    }

    InterfaceSet ( String interfaces[] ) {
        int length;
        this.data = clear();
        
        length = interfaces.length;
        for ( int c = 0 ; c < length ; c++ ) {
            data = add ( this.data, interfaces[c] );
            if ( data < 0 ) Netcap.error( "InterfaceSet.add:" + interfaces[c] );
            
            this.data = data;
        }
    }

    int toInt() 
    {
        return this.data;
    }
        
    private static native int clear();
    private static native int add( int data, String interfaceName );

    static 
    {
        Netcap.load();
    }
}
