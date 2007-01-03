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
import com.untangle.mvvm.tran.firewall.Parser;
import com.untangle.mvvm.tran.firewall.ParsingConstants;

public final class PortSingleMatcher extends PortDBMatcher
{
    private final int port;

    private PortSingleMatcher( int port )
    {
        this.port = port;
    }

    public boolean isMatch( int port )
    {
        return ( this.port == port );
    }

    public String toDatabaseString()
    {
        return toString();
    }
    
    public String toString()
    {
        return String.valueOf( this.port );
    }

    public static PortDBMatcher makeInstance( int port )
    {
        PortMatcherUtil pmu = PortMatcherUtil.getInstance();
        port = pmu.fixPort( port );
        return new PortSingleMatcher( port );
    }

    /* This is just for matching a list of interfaces */
    static final Parser<PortDBMatcher> PARSER = new Parser<PortDBMatcher>() 
    {
        public int priority()
        {
            return 10;
        }
        
        public boolean isParseable( String value )
        {
            return ( !value.contains( ParsingConstants.MARKER_SEPERATOR ) &&
                     !value.contains( PortMatcherUtil.MARKER_RANGE ));
        }
        
        public PortDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid port single matcher '" + value + "'" );
            }
            
            return makeInstance( PortMatcherUtil.getInstance().fixPort( value ));
        }
    };
}

