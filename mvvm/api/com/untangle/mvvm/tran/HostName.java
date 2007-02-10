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

package com.untangle.mvvm.tran;

import java.util.List;
import java.util.LinkedList;

import java.io.Serializable;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

public class HostName implements Serializable {
    private static final Pattern HOSTLABEL_MATCHER;
    /* From RFC 1035 */
    private static final int HOSTLABEL_MAXLEN = 63;
    private static final int HOSTNAME_MAXLEN  = 255;

    private static final HostName EMPTY_HOSTNAME = new HostName( "" );
    private static final List RESERVED_HOSTNAME_LIST = new LinkedList();

    
    private final String hostName;

    private HostName( String hostName )
    {
        /* Hostnames are not case sensitive RFC 1035 */
        this.hostName    = hostName.toLowerCase().trim();
    }

    public String toString()
    { 
        return hostName; 
    }

    public boolean isEmpty()
    {
        return ( this == EMPTY_HOSTNAME || hostName.length() == 0 );
    }

    public boolean isQualified()
    {
        return ( hostName.indexOf( "." ) > 0 );
    }

    public HostName unqualified()
    {
        int index = hostName.indexOf( "." ); 

        /* If the hostname is qualified, then return the substring, otherwise, return
         * the hostname itself */
        if ( index > 0 ) return new HostName( hostName.substring( 0, index ));
        
        return this;
    }

    public boolean isReserved()
    {
        return ( RESERVED_HOSTNAME_LIST.contains( hostName ));
    }

    /* The value here is just the host name which is a string, so pass these down to the string */
    public int hashCode()
    {
        /* hostname is always stored in lowercase */
        return hostName.hashCode();
    }

    public boolean equals( Object o )
    {
        if ( o instanceof HostName ) {
            return hostName.equalsIgnoreCase(((HostName)o).toString());
        }

        return false;
    }

    public static HostName parse( String input ) throws ParseException
    { 
        input = input.trim();

        if ( input.length() == 0 ) {
            return EMPTY_HOSTNAME;
        }
        
        if ( input.length() > HOSTNAME_MAXLEN ) {
            throw new ParseException( "A hostname label is limited to " + HOSTNAME_MAXLEN + " characters" );
        }

        if ( input.indexOf( ".." ) >= 0 ) {
            throw new ParseException( "A hostname does not contain \"..\" " + input );
        }
        
        String tmp[] = input.split( "\\." );
        
        if ( tmp.length == 0 ) {
            throw new ParseException( "A hostname must contain characters besides ." );
        }
        
        for ( int c = 0 ; c < tmp.length ; c++ ) {
            if ( tmp[c].length() > HOSTLABEL_MAXLEN ) {
                throw new ParseException( "A hostname label is limited to " + HOSTLABEL_MAXLEN + " characters" );
            }
            
            if ( !HOSTLABEL_MATCHER.matcher( tmp[c] ).matches()) {
                throw new ParseException( "A hostname label only contains numbers, letters and dashes: " + input );
            }
        }
        
        return new HostName( input );
    }

    public static HostName addDomain( HostName hostName, HostName domain )
    {
        return new HostName( hostName.toString() + "." + domain.toString());
    }

    public static HostName getEmptyHostName()
    {
        return EMPTY_HOSTNAME;
    }

    public static boolean equals( HostName name1, HostName name2 )
    {
        if ( name1 == null || name2 == null ) return ( name1 == name2 );
        
        return name1.equals( name2 );
    }

    static
    {
        Pattern p;

        try {
            /* According RFC2317, a host label must start a letter (allowing digits), and must end
             * in a digit. */
            p = Pattern.compile( "^[0-9A-Za-z]([-/_0-9A-Za-z]*[0-9A-Za-z])?$" );
        } catch ( PatternSyntaxException e ) {
            System.err.println( "Unable to intialize the host label pattern" );
            p = null;
        }

        HOSTLABEL_MATCHER = p;
        
        /* Local host is a reserved hostname */
        /* XXX */
        RESERVED_HOSTNAME_LIST.add( "localhost" );
        RESERVED_HOSTNAME_LIST.add( "mv-edgeguard" );
    }
}
