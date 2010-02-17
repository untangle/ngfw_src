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

package com.untangle.uvm.node.firewall.ip;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;
import com.untangle.uvm.node.firewall.ParsingConstants;

/**
 * An IPMatcher that matches a list of discontinous addresses.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public final class IPSetMatcher extends IPDBMatcher
{
    private static final long serialVersionUID = 2142806285574662136L;

    /* The set of addresses that match */
    private final Set<InetAddress> addressSet;

    /* The user/database representation of this matcher */
    private final String string;

    private IPSetMatcher( Set<InetAddress> addressSet, String string )
    {
        this.addressSet = addressSet;
        this.string  = string;
    }

    /**
     * Test if <param>address<param> matches this matcher.
     *
     * @param address The address to test.
     * @return True if <param>address</param> matches.
     */
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

    /**
     * Create a set matcher.
     *
     * @param addressArray The array of addresses that should match.
     * @return A SetMatcher that matches IP address from
     * <param>addressArray</param>.
     */
    public static IPDBMatcher makeInstance( InetAddress ... addressArray )
    {
        Set<InetAddress> addressSet = new HashSet<InetAddress>();

        for ( InetAddress address : addressArray ) {
            if ( address == null ) continue;
            addressSet.add( address );
        }

        return makeInstance( addressSet );
    }

    /**
     * Create a set matcher.
     *
     * @param addressArray The array of addresses that should match.
     * @return A RangeMatcher that matches IP address from
     * <param>addressArray</param>.
     */
    public static IPDBMatcher makeInstance( Set<InetAddress> addressSet ) 
    {        
        String user = "";

        for ( InetAddress address : addressSet ) {
            if ( user.length() != 0 ) user += " " + ParsingConstants.MARKER_SEPERATOR + " ";
            user += address.getHostAddress();
        }

        /* Create a copy so it can't change from underneath it */
        addressSet = Collections.unmodifiableSet( new HashSet<InetAddress>( addressSet ));
    
        return new IPSetMatcher( addressSet, user );
    }

    /* This is the parser for the set matchers. */
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

