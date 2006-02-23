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

package com.metavize.mvvm.tran.firewall.ip;

import java.net.InetAddress;

import com.metavize.mvvm.tran.IPaddr;

import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tran.firewall.Parser;
import com.metavize.mvvm.tran.firewall.ParsingConstants;

public final class IPLocalMatcher extends IPDBMatcher
{
    private static final IPLocalMatcher INSTANCE = new IPLocalMatcher();
    private static final String MARKER_LOCAL[] = { "local", "edgeguard" };
    
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
        /* This is kind of harsh because edgeguard would always get converted to local, but
         * it isn't too bad */
        return MARKER_LOCAL[0];
    }

    /* Set the addresses of the outside interface */
    public void setAddresses( InetAddress ... externalAddressArray )
    {
        if ( externalAddressArray == null ) {
            matcher = IPSimpleMatcher.getNilMatcher();
        } else {
            matcher = IPSetMatcher.makeInstance( externalAddressArray );
        }
    }

    public static IPLocalMatcher getInstance()
    {
        return INSTANCE;
    }

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
