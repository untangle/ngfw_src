/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.node;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class HostName implements Serializable {
    private static final Pattern HOSTLABEL_MATCHER;
    private static final Pattern HOSTLABEL_STRICT_MATCHER;
    /* From RFC 1035 */
    private static final int HOSTLABEL_MAXLEN = 63;
    private static final int HOSTNAME_MAXLEN  = 255;

    private static final HostName EMPTY_HOSTNAME = new HostName( "" );
    private static final List RESERVED_HOSTNAME_LIST = new LinkedList();

    private static final long serialVersionUID = -7181314697064348937L;

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

    // UI uses this one for system host name, etc.
    public static HostName parseStrict( String input ) throws ParseException
    { 
        return parseInternal(input, HOSTLABEL_STRICT_MATCHER);
    }

    // DB calls in here
    public static HostName parse( String input ) throws ParseException
    {
        return parseInternal(input, HOSTLABEL_MATCHER);
    }

    private static HostName parseInternal( String input, Pattern goodNamePattern )
        throws ParseException
    {
        if ( input == null ) input = "";

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
            
            if ( !goodNamePattern.matcher( tmp[c] ).matches()) {
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
        Pattern p, q;

        try {
            /* According RFC2317, a host label must start a letter (allowing digits), and must end
             * in a digit. */
            p = Pattern.compile( "^[0-9A-Za-z]([-/_0-9A-Za-z]*[0-9A-Za-z])?$" );
            q = Pattern.compile( "^[0-9A-Za-z]([-0-9A-Za-z]*[0-9A-Za-z])?$" );
        } catch ( PatternSyntaxException e ) {
            System.err.println( "Unable to intialize the host label pattern" );
            p = null;
            q = null;
        }

        HOSTLABEL_MATCHER = p;
        HOSTLABEL_STRICT_MATCHER = q;
        
        /* Local host is a reserved hostname */
        /* XXX */
        RESERVED_HOSTNAME_LIST.add( "localhost" );
        RESERVED_HOSTNAME_LIST.add( "mv-edgeguard" );
    }
}
