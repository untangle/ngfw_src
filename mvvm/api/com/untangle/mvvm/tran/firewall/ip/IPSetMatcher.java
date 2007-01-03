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

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.untangle.mvvm.tran.IPaddr;

import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.firewall.Parser;
import com.untangle.mvvm.tran.firewall.ParsingConstants;

public final class IPSetMatcher extends IPDBMatcher
{
    private static final String MARKER_RANGE = "-";

    private final Set<InetAddress> addressSet;
    private final String string;

    private IPSetMatcher( Set<InetAddress> addressSet, String string )
    {
        this.addressSet = addressSet;
        this.string  = string;
    }

    public boolean isMatch( InetAddress address )
    {
        return ( this.addressSet.contains( address ));
    }

    public String toDatabaseString()
    {
        return toString();
    }
    
    public String toString()
    {
        return this.string;
    }

    public static IPDBMatcher makeInstance( InetAddress ... addressArray )
    {
        Set<InetAddress> addressSet = new HashSet<InetAddress>();

        for ( InetAddress address : addressArray ) {
            if ( address == null ) continue;
            addressSet.add( address );
        }

        return makeInstance( addressSet );
    }

    public static IPDBMatcher makeInstance( Set<InetAddress> addressSet ) 
    {
        IPMatcherUtil imu = IPMatcherUtil.getInstance();
        
        if ( addressSet == null ) {
            
        }
        
        
        String user = "";

        for ( InetAddress address : addressSet ) {
            if ( user.length() != 0 ) user += " " + ParsingConstants.MARKER_SEPERATOR + " ";
            user += address.getHostAddress();
        }

        /* XXX This should make a copy of the set */
        addressSet = Collections.unmodifiableSet( new HashSet<InetAddress>( addressSet ));
    
        return new IPSetMatcher( addressSet, user );
    }

    /* This is just for matching a list of interfaces */
    static final Parser<IPDBMatcher> PARSER = new Parser<IPDBMatcher>() 
    {
        public int priority()
        {
            return 8;
        }
        
        public boolean isParseable( String value )
        {
            return ( value.contains( ParsingConstants.MARKER_SEPERATOR ));
        }
        
        public IPDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid ip set matcher '" + value + "'" );
            }
            
            String ipArray[] = value.split( ParsingConstants.MARKER_SEPERATOR );
            Set<InetAddress> addressSet = new HashSet<InetAddress>();

            for ( String ipString : ipArray ) {
                ipString = ipString.trim();
                
                try {
                    addressSet.add ( IPaddr.parse( ipString ).getAddr());
                } catch ( UnknownHostException e ) {
                    throw new ParseException( "Unable to create an ip set matcher, the ip address '" + 
                                              ipString + "' is invalid", e );
                }
            }

            return makeInstance( addressSet );
        }
    };
}

