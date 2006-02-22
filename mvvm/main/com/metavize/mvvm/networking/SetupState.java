/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.networking;

import java.io.Serializable;

import java.util.Map;
import java.util.HashMap;

import com.metavize.mvvm.tran.ParseException;

public final class SetupState implements Serializable
{
    private static final Map<Integer,SetupState> TYPE_TO_SETUP = new HashMap<Integer,SetupState>();
    private static final Map<String,SetupState>  NAME_TO_SETUP = new HashMap<String,SetupState>();

    /* The settings have not been upgraded yet. */
    public static final SetupState NETWORK_SHARING  = new SetupState( 1, "deprecated" );
        
    /* The user hasn't selected basic or advanced. */
    public static final SetupState UNCONFIGURED  = new SetupState( 2, "Unconfigured" );
        
    /* The user wants a simple interface */
    public static final SetupState BASIC = new SetupState( 3, "Basic" );
    
    /* The user is ambitious, and wants to take full advantage of network spaces. */
    public static final SetupState ADVANCED = new SetupState( 4, "Advanced" );

    private static final SetupState ENUMERATION[] =
    {
        NETWORK_SHARING, UNCONFIGURED, BASIC, ADVANCED
    };

    private final int type;
    private final String name;

    private SetupState( int type, String name )
    {
        this.type  = type;
        this.name  = name;
    }

    public int getType()
    {
        return this.type;
    }

    public String getName()
    {
        return this.name;
    }

    public String toString()
    {
        return this.name;
    }

    public static SetupState[] getEnumeration()
    {
        return ENUMERATION;
    }

    public static SetupState getDefault()
    {
        return ENUMERATION[0];
    }

    public static SetupState getInstance( int type ) throws ParseException
    {
        SetupState instance = TYPE_TO_SETUP.get( type );
        if ( instance == null ) throw new ParseException( "Invalid setup state["+ type + "]." );
        return instance;
    }

    @Override
    public boolean equals( Object o )
    {
        if (!(o instanceof SetupState )) return false;

        SetupState ss = (SetupState)o;
        return ( this.type == ss.type );
    }

    /* have fun with that one(type should not be 0) */
    public int hashCode()
    {
        return ( this.type * 103423 ) %  104659;
    }

    
    static
    {
        for ( SetupState em : ENUMERATION ) {
            TYPE_TO_SETUP.put( em.getType(), em );
            NAME_TO_SETUP.put( em.getName(), em );
        }
    }
}
