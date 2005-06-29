/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.tran.firewall;

import java.io.Serializable;

import com.metavize.mvvm.tapi.Protocol;

import com.metavize.mvvm.tran.ParseException;

/**
 * The class <code>ProtocolMatcher</code> represents a class for filtering on the Protocol of a
 * session.
 *
 * @author <a href="mailto:rbscott@metavize.com">rbscott</a>
 * @version 1.0
 */
public final class ProtocolMatcher implements Serializable
{
    public static final String MARKER_TCP       = "TCP";
    public static final String MARKER_UDP       = "UDP";
    public static final String MARKER_WILDCARD  = MatcherStringConstants.WILDCARD;
    public static final String MARKER_ALL       = "TCP & UDP";
    public static final String MARKER_PING      = "PING";
    public static final String MARKER_SEP       = MatcherStringConstants.SEPERATOR;
    private static final String MARKER_NOTHING  = MatcherStringConstants.NOTHING;

    private static final String[] ENUMERATION = { MARKER_ALL, MARKER_UDP, MARKER_TCP, MARKER_PING };

    public static final ProtocolMatcher MATCHER_ALL  = new ProtocolMatcher( true, true );
    public static final ProtocolMatcher MATCHER_TCP  = new ProtocolMatcher( true, false );
    public static final ProtocolMatcher MATCHER_UDP  = new ProtocolMatcher( false, true );
    public static final ProtocolMatcher MATCHER_PING = new ProtocolMatcher();
    public static final ProtocolMatcher MATCHER_NIL  = new ProtocolMatcher( false, false );
    
    public final boolean isTcpEnabled;
    public final boolean isUdpEnabled;
    public final boolean isPingEnabled;

    private ProtocolMatcher( boolean tcp, boolean udp ) {
        isTcpEnabled  = tcp;
        isUdpEnabled  = udp;
        isPingEnabled = false;
    }

    /* This is just for PING matching */
    private ProtocolMatcher()
    {
        isTcpEnabled  = false;
        isUdpEnabled  = false;
        isPingEnabled = true;
    }
    
    public boolean isMatch( Protocol protocol ) {
        if (( protocol == Protocol.TCP )  && isTcpEnabled )
            return true;

        if (( protocol == Protocol.UDP )  && isUdpEnabled )
            return true;

        /* Right now Ping is a UDP session */
        if (( protocol == Protocol.UDP ) && isPingEnabled )
            return true;

        return false;
    }
    
    
    public String toString()
    {
        if (( this.equals( MATCHER_ALL )) || ( isTcpEnabled && isUdpEnabled )) {
            return MARKER_ALL;
        } else if (( this.equals( MATCHER_PING )) || ( isPingEnabled )) {
            return MARKER_PING;
        } else if (( this.equals( MATCHER_NIL )) || ( !isTcpEnabled && !isUdpEnabled )) {
            return MARKER_NOTHING;
        }
        
        if ( isTcpEnabled ) {
            return MARKER_TCP;
        }
        
        /* That is every combination but UDP */
        return MARKER_UDP;
    }

    public boolean equals( Object o )
    {
        if (!( o instanceof ProtocolMatcher )) return false;
        
        ProtocolMatcher p = (ProtocolMatcher)o;
        return (( p.isTcpEnabled == this.isTcpEnabled ) && ( p.isUdpEnabled == this.isUdpEnabled ) &&
                ( p.isPingEnabled == this.isPingEnabled ));
    }

    /**
     * An ProtocolMatcher can be specified in one of the following formats:
     * 1. (I|O)[,(I|O)]* Inside or outside (one of each) eg "I,O" or "I" or "O"
     * 2. * : Wildcard matches everything.
     * 3. ! : Nothing matches nothing
     */
    public static ProtocolMatcher parse( String str ) throws ParseException
    {
        str = str.trim();
        boolean isTcpEnabled  = false;
        boolean isUdpEnabled  = false;
        boolean isPingEnabled = false;
        
        if ( str.indexOf( MARKER_SEP ) > 0 ) {
            String strArray[] = str.split( MARKER_SEP );
            for ( int c = 0 ; c < strArray.length ; c++ ) {
                if ( strArray[c].equalsIgnoreCase( MARKER_TCP )) {
                    isTcpEnabled = true;
                } else if ( strArray[c].equalsIgnoreCase( MARKER_UDP )) {
                    isUdpEnabled = true;
                } else {
                    throw new ParseException( "Invalid ProtocolMatcher at \"" + strArray[c] + "\"" );
                }
            }
        } else if ( str.equalsIgnoreCase( MARKER_WILDCARD ) || str.equalsIgnoreCase( MARKER_ALL )) {
            return  MATCHER_ALL;
        } else if ( str.equalsIgnoreCase( MARKER_NOTHING )) {
            return MATCHER_NIL;
        } else if ( str.equalsIgnoreCase( MARKER_TCP ))  {
            isTcpEnabled = true;
        } else if ( str.equalsIgnoreCase( MARKER_UDP )) {
            isUdpEnabled = true;
        } else if ( str.equalsIgnoreCase( MARKER_PING )) { 
            isPingEnabled = true;
        } else {
            throw new ParseException( "Invalid ProtocolMatcher at \"" + str + "\"" );
        }
        
        if ( isPingEnabled ) {
            return MATCHER_PING;
        } else if ( isTcpEnabled && isUdpEnabled ) {
            return MATCHER_ALL;
        } else if ( !isTcpEnabled && !isUdpEnabled ) {
            return MATCHER_NIL;
        } else if ( isTcpEnabled ) {
            return MATCHER_TCP;
        }
        return MATCHER_UDP;
    }

    public static String[] getProtocolEnumeration()
    {
        return ENUMERATION;
    }

    public static String getProtocolDefault()
    {
        return ENUMERATION[0];
    }
}
