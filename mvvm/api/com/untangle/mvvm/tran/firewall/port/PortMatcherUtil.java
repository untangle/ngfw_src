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

class PortMatcherUtil
{
    static final int INADDRSZ = 4;

    static final int PORT_MIN = 0;
    static final int PORT_MAX = 0xFFFF;

    static final String MARKER_RANGE  = "-";

    private static final PortMatcherUtil INSTANCE = new PortMatcherUtil();
    
    private PortMatcherUtil()
    {
    }

    boolean isValidPort( int port )
    {
        return (( PORT_MIN <= port ) && ( port <= PORT_MAX ));
    }

    int fixPort( int port )
    {
        if ( port < PORT_MIN ) port = PORT_MIN;
        if ( port > PORT_MAX ) port = PORT_MAX;
        
        return port;
    }

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
