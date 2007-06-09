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

package com.untangle.mvvm.user;

import com.untangle.mvvm.tran.ParseException;

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