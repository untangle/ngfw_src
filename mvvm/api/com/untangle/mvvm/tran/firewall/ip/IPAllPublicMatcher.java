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
import com.untangle.mvvm.tran.firewall.ParsingConstants;

public final class IPAllPublicMatcher extends IPDBMatcher
{
    private static final IPAllPublicMatcher INSTANCE = new IPAllPublicMatcher();
    private static final String MARKER_ALL_PUBLIC[] = { "external address & aliases", "all external addresses" };
    
    private static IPDBMatcher matcher = IPSimpleMatcher.getNilMatcher();

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

    /* Set the addresses of the outside interface */
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
