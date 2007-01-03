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

package com.untangle.mvvm.tran.firewall.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.firewall.Parser;
import com.untangle.mvvm.tran.firewall.ParsingConstants;

public final class UserSetMatcher extends UserDBMatcher
{
    private static final long serialVersionUID = -5205089936763240676L;

    private final Set<String> userSet;
    private final String string;

    private UserSetMatcher( Set<String> userSet, String string )
    {
        this.userSet = userSet;
        this.string  = string;
    }

    public boolean isMatch( String user )
    {
        return ( this.userSet.contains( user.trim().toLowerCase()));
    }

    public List<String> toDatabaseList()
    {
        return Collections.unmodifiableList( new ArrayList<String>( userSet ));
    }
    
    public String toString()
    {
        return this.string;
    }

    public static UserDBMatcher makeInstance( String ... userArray )
    {
        Set<String> userSet = new HashSet<String>();

        for ( String user : userArray ) userSet.add( user );

        return makeInstance( userSet );
    }

    public static UserDBMatcher makeInstance( Set<String> userSet ) 
    {
        if ( userSet == null ) return UserSimpleMatcher.getNilMatcher();
                
        String value = "";

        for ( String user  : userSet ) {
            if ( value.length() != 0 ) value += " " + UserMatcherConstants.MARKER_SEPERATOR + " ";
            value += value;
        }

        userSet = Collections.unmodifiableSet( userSet );
    
        return new UserSetMatcher( userSet, value );
    }

    /* This is just for matching a list of interfaces */
    static final Parser<UserDBMatcher> PARSER = new Parser<UserDBMatcher>() 
    {
        public int priority()
        {
            return 8;
        }
        
        public boolean isParseable( String value )
        {
            return ( value.contains( UserMatcherConstants.MARKER_SEPERATOR ));
        }
        
        public UserDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid user set matcher '" + value + "'" );
            }
            
            String userArray[] = value.split( UserMatcherConstants.MARKER_SEPERATOR );
            Set<String> userSet = new HashSet<String>();
            
            /* ??? using lower case because assuming the usernames are case insentive */
            for ( String userString : userArray ) userSet.add ( userString.trim().toLowerCase());

            return makeInstance( userSet );
        }
    };
}

