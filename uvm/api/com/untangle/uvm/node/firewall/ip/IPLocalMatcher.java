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

package com.untangle.uvm.node.firewall.ip;

import java.net.InetAddress;

import com.untangle.uvm.node.IPaddr;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;

/**
 * An IPMatcher that matches the primary address of the external
 * network space.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public final class IPLocalMatcher extends IPDBMatcher
{
    private static final IPLocalMatcher INSTANCE = new IPLocalMatcher();

    /* Possible database and user representations of for this value.
     * This array should only been added to, items should never be
     * removed.  If an item must be removed, then it will require a
     * schema converter to update any values that may be in a
     * database */
    private static final String MARKER_LOCAL[] = { "external address", "local", "edgeguard" };

    /* The matcher to use when testing for match, this is updated when the address changes. */
    private static IPDBMatcher matcher = IPSimpleMatcher.getNilMatcher();

    /**
     * Test if <param>address<param> matches this matcher.
     *
     * @param address The address to test.
     * @return True if <param>address</param> is the primary address
     * on the external interface.
     */
    public boolean isMatch( InetAddress address )
    {
        return matcher.isMatch( address );
    }
    
    public String toString()
    {
        return toDatabaseString();
    }

    public String toDatabaseString()
    {
        /* This is kind of harsh because edgeguard would always get converted to local, but
         * it isn't too bad */
        return MARKER_LOCAL[0];
    }

    /**
     * Update the primary external addresses.
     *
     * @param externalAddress The new external address.
     */
    void setAddress( InetAddress externalAddress )
    {
        if ( externalAddress == null ) {
            matcher = IPSimpleMatcher.getNilMatcher();
        } else {
            matcher = IPSingleMatcher.makeInstance( externalAddress );
        }
    }

    public static IPLocalMatcher getInstance()
    {
        return INSTANCE;
    }

    /**
     * The parser for the local matcher */
    static final Parser<IPDBMatcher> PARSER = new Parser<IPDBMatcher>() 
    {
        public int priority()
        {
            return 1;
        }
        
        public boolean isParseable( String value )
        {
            for ( String marker : MARKER_LOCAL ) {
                if ( marker.equalsIgnoreCase( value )) return true;
            }
            
            return false;
        }
        
        public IPDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid ip local matcher '" + value + "'" );
            }
            
            /* If it is parseable, then it is ready to go */
            return INSTANCE;
        }
    };

}
