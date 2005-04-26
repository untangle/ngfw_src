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

import com.metavize.mvvm.argon.IntfConverter;

import java.io.Serializable;


/**
 * The class <code>IntfMatcher</code> represents a class for filtering on one of the interfaces
 * for a session.
 *
 * @author <a href="mailto:rbscott@metavize.com">rbscott</a>
 * @version 1.0
 */
public class IntfMatcher  implements Serializable
{
    public static final String MARKER_INSIDE    = "I";
    public static final String MARKER_OUTSIDE   = "O";
    public static final String MARKER_WILDCARD  = MatcherStringConstants.WILDCARD;
    public static final String MARKER_SEP       = MatcherStringConstants.SEPERATOR;
    private static final String MARKER_NOTHING  = MatcherStringConstants.NOTHING;
    
    /* XXX this just won't work at all for three interfaces */
    public static final IntfMatcher MATCHER_ALL = new IntfMatcher( true, true );
    public static final IntfMatcher MATCHER_IN  = new IntfMatcher( true, false );
    public static final IntfMatcher MATCHER_OUT = new IntfMatcher( false, true );
    public static final IntfMatcher MATCHER_NIL = new IntfMatcher( false, false );

    public final boolean isInsideEnabled;
    public final boolean isOutsideEnabled;

    private IntfMatcher( boolean inside, boolean outside ) {
        isInsideEnabled  = inside;
        isOutsideEnabled = outside;
    }
    
    public boolean isMatch( byte intf ) {
        if (( intf == IntfConverter.INSIDE )  && isInsideEnabled )
            return true;

        if (( intf == IntfConverter.OUTSIDE ) && isOutsideEnabled )
            return true;

        return false;
    }

    public String toString()
    {
        if (( this == MATCHER_ALL ) || ( isOutsideEnabled && isInsideEnabled )) {
            return MARKER_WILDCARD;
        } else if (( this == MATCHER_NIL ) || ( !isOutsideEnabled && !isInsideEnabled )) {
            return MARKER_NOTHING;
        }

        String intf = "";

        if ( isOutsideEnabled ) {
            intf += MARKER_OUTSIDE;
        }

        if ( isInsideEnabled ) {
            intf += MARKER_INSIDE;
        }
        
        return intf;        
    }

    /**
     * An IntfMatcher can be specified in one of the following formats:
     * 1. (I|O)[,(I|O)]* Inside or outside (one of each) eg "I,O" or "I" or "O"
     * 2. * : Wildcard matches everything.
     * 3. ! : Nothing matches nothing
     */
    public static IntfMatcher parse( String str ) throws IllegalArgumentException
    {
        str = str.trim();
        boolean isInsideEnabled  = false;
        boolean isOutsideEnabled = false;

        
        if ( str.indexOf( MARKER_SEP ) > 0 ) {
            String strArray[] = str.split( MARKER_SEP );
            for ( int c = 0 ; c < strArray.length ; c++ ) {
                if ( strArray[c].equalsIgnoreCase( MARKER_INSIDE )) {
                    isInsideEnabled = true;
                } else if ( strArray[c].equalsIgnoreCase( MARKER_OUTSIDE )) {
                    isOutsideEnabled = true;
                } else {
                    throw new IllegalArgumentException( "Invalid IntfMatcher at \"" + strArray[c] + "\"" );
                }
            }
        } else if ( str.equalsIgnoreCase( MARKER_WILDCARD )) {
            return  MATCHER_ALL;
        } else if ( str.equalsIgnoreCase( MARKER_NOTHING )) {
            return MATCHER_NIL;
        } else if ( str.equalsIgnoreCase( MARKER_OUTSIDE ))  {
            isOutsideEnabled = true;
        } else if ( str.equalsIgnoreCase( MARKER_INSIDE )) {
            isInsideEnabled = true;
        } else {
            throw new IllegalArgumentException( "Invalid IntfMatcher at \"" + str + "\"" );
        }
        
        if ( isOutsideEnabled && isInsideEnabled ) {
            return MATCHER_ALL;
        } else if ( isOutsideEnabled && isInsideEnabled ) {
            return MATCHER_NIL;
        } else if ( isOutsideEnabled ) {
            return MATCHER_OUT;
        }
        return MATCHER_IN;
    }
}
