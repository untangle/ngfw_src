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
import java.net.UnknownHostException;

import com.untangle.mvvm.tran.IPaddr;

import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.firewall.Parser;
import com.untangle.mvvm.tran.firewall.ParsingConstants;

/**
 * An IPMatcher that matches a single IP address.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public final class IPSingleMatcher extends IPDBMatcher
{
    /* The address that matches */
    private final InetAddress address;

    private IPSingleMatcher( InetAddress address )
    {
        this.address = address;
    }

    /**
     * Determine if <param>address</param> matches the address for
     * this matcher.
     *
     * @param address The address to test.
     * @return True if <param>address</param> matches the address for
     * this matcher.
     */
    public boolean isMatch( InetAddress address )
    {
        return this.address.equals( address );
    }

    public String toDatabaseString()
    {
        return toString();
    }
    
    public String toString()
    {
        return this.address.getHostAddress();
    }

    /**
     * Create a single matcher.
     *
     * @param address The address that should match.
     * @return An IPMatcher that matches IP address <param>address</param>
     */
    public static IPDBMatcher makeInstance( InetAddress address )
    {
        if ( address == null ) throw new NullPointerException( "Null address" );
        return new IPSingleMatcher( address );
    }

    /**
     * Create a single matcher, uses an IPaddr instead of an
     * InetAddress.
     *
     * @param address The address that should match.
     * @return An IPMatcher that matches IP address <param>address</param>
     */
    public static IPDBMatcher makeInstance( IPaddr address )
    {
        return makeInstance( address.getAddr());
    }

    /* This is the parser for a single matcher */
    static final Parser<IPDBMatcher> PARSER = new Parser<IPDBMatcher>() 
    {
        public int priority()
        {
            return 10;
        }
        
        public boolean isParseable( String value )
        {
            return ( !value.contains( ParsingConstants.MARKER_SEPERATOR ) &&
                     !value.contains( IPMatcherUtil.MARKER_RANGE ) &&
                     !value.contains( IPMatcherUtil.MARKER_SUBNET ));
        }
        
        public IPDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid ip single matcher '" + value + "'" );
            }
            
            try {
                return makeInstance( IPaddr.parse( value ));
            } catch ( UnknownHostException e ) {
                throw new ParseException( e );
            }
        }
    };
}

