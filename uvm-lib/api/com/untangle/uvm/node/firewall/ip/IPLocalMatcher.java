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
    private static final long serialVersionUID = -5160105766158472713L;

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
