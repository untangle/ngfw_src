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

package com.untangle.uvm.node.firewall.user;

import java.util.Collections;
import java.util.List;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;

public final class UserSingleMatcher extends UserDBMatcher
{
    private static final long serialVersionUID = 8513194248419629420L;

    private final String user;

    private UserSingleMatcher( String user )
    {
        this.user = user;
    }

    public boolean isMatch( String user )
    {
        /* ignore case ??? */
        return ( this.user.equalsIgnoreCase( user ));
    }

    public List<String> toDatabaseList()
    {
        return Collections.nCopies( 1, toString());
    }
        
    public String toString()
    {
        return this.user;
    }

    public static UserDBMatcher makeInstance( String user )
    {
        return new UserSingleMatcher( user );
    }

    /* This is just for matching a list of interfaces */
    static final Parser<UserDBMatcher> PARSER = new Parser<UserDBMatcher>() 
    {
        public int priority()
        {
            return 10;
        }
        
        public boolean isParseable( String value )
        {
            return !value.contains( UserMatcherConstants.MARKER_SEPERATOR );
        }
        
        public UserDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid port single matcher '" + value + "'" );
            }
            
            return makeInstance( value );
        }
    };
}

