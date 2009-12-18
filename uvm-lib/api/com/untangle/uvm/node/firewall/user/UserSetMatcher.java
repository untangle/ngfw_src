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

package com.untangle.uvm.node.firewall.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.Set;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;

public final class UserSetMatcher extends UserDBMatcher
{
    private static final long serialVersionUID = -5205089936763240676L;

    private final Set<String> userSet;
    private final String string;
    private final boolean matchUnauthenticated;

    private UserSetMatcher( Set<String> userSet, String string )
    {
        this( userSet, string, false );
    }

    private UserSetMatcher( Set<String> userSet, String string, boolean matchUnauthenticated )
    {
        this.userSet = userSet;
        this.string  = string;
        this.matchUnauthenticated = matchUnauthenticated;
    }

    public boolean isMatch( String user )
    {
        if ( user == null ) {
            return this.matchUnauthenticated;
        }

        return ( this.userSet.contains( user.toLowerCase()));
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
        return makeInstance( false, userArray );
    }
    
    public static UserDBMatcher makeInstance( boolean matchUnauthenticated, String ... userArray )
    {
        Set<String> userSet = new TreeSet<String>();

        for ( String user : userArray ) userSet.add( user );

        return makeInstance( userSet, matchUnauthenticated );
    }

    public static UserDBMatcher makeInstance( Set<String> userSet ) 
    {
        return makeInstance( userSet, false );
    }

    public static UserDBMatcher makeInstance( Set<String> userSet, boolean matchUnauthenticated )
    {
        if ( userSet == null ) return UserSimpleMatcher.getNilMatcher();
                
        StringBuilder value = new StringBuilder();
        int i = 0;
        for ( String user  : userSet ) {
            if ( user.equals( UserMatcherConstants.MARKER_AUTHENTICATED ))
            if ( i > 0 )
                value.append(" ").append(UserMatcherConstants.MARKER_SEPERATOR).append(" ");
            value.append(user);
            i++;
        }
        
        /* If matchUnauthenticated is not true, check if it is in the set of users (done in reverse to always remove the unauthenticated marker */ 
        matchUnauthenticated = userSet.remove( UserMatcherConstants.MARKER_UNAUTHENTICATED ) || matchUnauthenticated;
        
        if ( userSet.remove( UserMatcherConstants.MARKER_AUTHENTICATED )) {
            if ( matchUnauthenticated )  {
                return UserSimpleMatcher.getAllMatcher();
            } else {
                return UserSimpleMatcher.getAuthenticatedMatcher();
            }
        }

        userSet = Collections.unmodifiableSet( userSet );
    
        return new UserSetMatcher( userSet, value.toString(), matchUnauthenticated );        
    }

    /* This is just for matching a list of users */
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
            Set<String> userSet = new TreeSet<String>();
            
            /* ??? using lower case because assuming the usernames are case insentive */
            for ( String userString : userArray ) userSet.add ( userString.trim().toLowerCase());

            return makeInstance( userSet );
        }
    };
}

