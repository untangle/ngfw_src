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

package com.untangle.mvvm.tran.firewall.ip;

import java.net.InetAddress;

import com.untangle.mvvm.tran.IPaddr;

import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.firewall.Parser;

/**
 * An IPMatcher that matches all of the addresses assigned to the
 * external network space.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public final class IPAllPublicMatcher extends IPDBMatcher
{
    private static final IPAllPublicMatcher INSTANCE = new IPAllPublicMatcher();

    /* Possible database and user representations of for this value.
     * This array should only been added to, items should never be
     * removed.  If an item must be removed, then it will require a
     * schema converter to update any values that may be in a
     * database */
    private static final String MARKER_ALL_PUBLIC[] = { "external address & aliases", "all external addresses" };
    
    /* The matcher to use when testing for match, this is updated when the address changes. */
    private static IPDBMatcher matcher = IPSimpleMatcher.getNilMatcher();

    /**
     * Test if <param>address<param> matches this matcher.
     *
     * @param address The address to test.
     * @return True if <param>address</param> matches one of the public
     * addresses.
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
        return MARKER_ALL_PUBLIC[0];
    }

    /**
     * Update the set of external addresses.
     *
     * @param externalAddressArray The new array of external addresses
     */
    void setAddresses( InetAddress ... externalAddressArray )
    {
        if (( externalAddressArray == null ) || ( externalAddressArray.length == 0 )) {
            matcher = IPSimpleMatcher.getNilMatcher();
        } else {
            matcher = IPSetMatcher.makeInstance( externalAddressArray );
        }
    }

    public static IPAllPublicMatcher getInstance()
    {
        return INSTANCE;
    }

    /**
     * The parser for an external address matcher */
    static final Parser<IPDBMatcher> PARSER = new Parser<IPDBMatcher>() 
    {
        public int priority()
        {
            return 2;
        }
        
        public boolean isParseable( String value )
        {
            for ( String marker : MARKER_ALL_PUBLIC ) {
                if ( marker.equalsIgnoreCase( value )) return true;
            }
            
            return false;
        }
        
        public IPDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid ip all public matcher '" + value + "'" );
            }
            
            /* If it is parseable, then it is ready to go */
            return INSTANCE;
        }
    };
}
