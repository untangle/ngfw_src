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

public final class IPLocalMatcher extends IPDBMatcher
{
    private static final IPLocalMatcher INSTANCE = new IPLocalMatcher();
    private static final String MARKER_LOCAL[] = { "external address", "local", "edgeguard" };
    
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
