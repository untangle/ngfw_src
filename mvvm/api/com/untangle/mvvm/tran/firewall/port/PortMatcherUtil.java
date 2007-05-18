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

package com.untangle.mvvm.tran.firewall.port;

import com.untangle.mvvm.tran.ParseException;

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
