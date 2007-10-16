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

package com.untangle.uvm.node.firewall.port;

import com.untangle.uvm.node.ParseException;

/**
 * A group of utilities for building port matchers.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
class PortMatcherUtil
{
    /* The minimum port */
    static final int PORT_MIN = 0;
    
    /* The maximum port */
    static final int PORT_MAX = 0xFFFF;

    /* The marker used in the string representation port range. */
    static final String MARKER_RANGE  = "-";

    private static final PortMatcherUtil INSTANCE = new PortMatcherUtil();
    
    private PortMatcherUtil()
    {
    }

    /**
     * Determine if <param>port</param> is a valid port.
     *
     * @param port Port to test
     * @return True if port is valid, false otherwise.
     */
    boolean isValidPort( int port )
    {
        return (( PORT_MIN <= port ) && ( port <= PORT_MAX ));
    }

    /**
     * Update <param>port</param> so it is in the range of valid ports
     *
     * @param port The port to update.
     * @return The updated port truncated into the range PORT_MIN ->
     * PORT_MAX.
     */
    int fixPort( int port )
    {
        if ( port < PORT_MIN ) port = PORT_MIN;
        if ( port > PORT_MAX ) port = PORT_MAX;
        
        return port;
    }

    /**
     * Parse a string representation of a port and then put it into
     * the proper range.
     *
     * @param port The port to convert.
     * @return The updated port truncated into the range PORT_MIN ->
     * PORT_MAX.
     * @exception ParseException If <param>port</param> is not a valid
     * number.
     */
    int fixPort( String port ) throws ParseException
    {
        try {
            port = port.trim();
            return fixPort( Integer.parseInt( port ));
        } catch ( NumberFormatException e ) {
            throw new ParseException( "Invalid port: '" + port + "'" );
        }
    }

    static PortMatcherUtil getInstance()
    {
        return INSTANCE;
    }
}
