/*
 * $HeadURL$
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

public class ResetCrumb extends Crumb
{
    private static final ResetCrumb ACKED     = new ResetCrumb();
    private static final ResetCrumb NOT_ACKED = new ResetCrumb();

    private ResetCrumb()
    {
    }
    
    public void raze()
    {
    }

    /**
     * Determine whether or not this is a reset crumb from an ACKED session
     * or from an unacked session.
     */
    public boolean acked()
    {
        return ( this == ACKED ) ? true : false;
    }

    public int type()
    { 
        return TYPE_RESET;
    }

    /**
     * Get the acked reset crumb.
     */
    public static ResetCrumb getInstance()
    {
        return ACKED;
    }

    /**
     * Get either the acked or non-acked reset crumb 
     */
    public static ResetCrumb getInstance( boolean acked )
    {
        return ( acked ) ? ACKED : NOT_ACKED;
    }

    public static ResetCrumb getInstanceNotAcked() 
    {
        return NOT_ACKED;
    }

}
