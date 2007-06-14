/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.jvector;

public class ShutdownCrumb extends Crumb
{
    /**
     * A normal shutdown crumb 
     */
    private static final ShutdownCrumb INSTANCE = new ShutdownCrumb();

    /**
     * An expired/dead session shutdown crumb.  In an expired session, vectoring
     * is completed, and it is infeasible to send and more crumbs 
     */
    private static final ShutdownCrumb EXPIRED  = new ShutdownCrumb();

    private ShutdownCrumb() 
    {
    }
    
    public void raze()
    {
    }
    
    public boolean isExpired()
    {
        return ( this == EXPIRED ) ? true : false;
    }

    public int type()
    { 
        return TYPE_SHUTDOWN; 
    }

    public static ShutdownCrumb getInstance() 
    {
        return INSTANCE;
    }

    public static ShutdownCrumb getInstance( boolean isExpired ) 
    {
        return ( isExpired ) ? EXPIRED : INSTANCE;
    }
    
    public static ShutdownCrumb getInstanceExpired()
    {
        return EXPIRED;
    }
}
