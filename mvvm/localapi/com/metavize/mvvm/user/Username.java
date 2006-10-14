/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.user;

import com.metavize.mvvm.tran.ParseException;

public final class Username
{
    private final String username;

    private Username( String username )
    {
        this.username = username.toLowerCase();
    }

    public String getUsername()
    {
        return this.username;
    }

    public String toString()
    {
        return this.username;
    }

    public boolean equals( Object o )
    {
        if ( !( o instanceof Username )) return false;
        
        return this.username.equalsIgnoreCase(((Username)o).username );
    }

    public int hashCode()
    {
        return this.username.hashCode();
    }

    public static Username parse( String value ) throws ParseException
    {
        /* XXX Look for valid characters */
        value = value.trim();

        return new Username( value );
    }
}