/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: IPMaddr.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.mvvm.tran;

import java.io.Serializable;

public class DomainName implements Serializable {
    private final String domainName;

    private static final DomainName EMPTY_DOMAINNAME = new DomainName( "" );
    
    /* From RFC 1035 */
    private static final int DOMAINNAME_MAXLEN = 255;
    
    private DomainName( String domainName )
    {
        /* Hostnames are not case sensitive RFC 1035 */
        this.domainName = domainName.toLowerCase().trim();
    }

    public String toString()
    { 
        return domainName;
    }

    public boolean isEmpty()
    {
        return ( this == EMPTY_DOMAINNAME || domainName.length() == 0 );
    }

    public static DomainName parse( String input ) throws IllegalArgumentException 
    { 
        input = input.trim();
        
        if ( input.length() > DOMAINNAME_MAXLEN ) {
            throw new IllegalArgumentException( "A domain name is limited to: " + DOMAINNAME_MAXLEN + " characters" );
        }
        
        String tmp[] = input.split( "\\." );

        for ( int c = 0 ; c < tmp.length ; c++ ) {
            if ( !HostName.isValid( tmp[c] )) {
                throw new IllegalArgumentException( "Invalid domainname component: " + tmp[c] );
            }
        }
        
        return new DomainName( input );
    }

    public static DomainName getEmptyDomainName()
    {
        return EMPTY_DOMAINNAME;
    }
}
