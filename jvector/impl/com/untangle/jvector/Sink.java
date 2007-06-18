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

public abstract class Sink
{
    protected int pointer;
    
    protected int snk_ptr() 
    { 
        return pointer;
    }

    protected abstract int send_event(Crumb o);
    protected abstract int shutdown();

    /**
     * This function can be called after vectoring is completed.  This can
     * guarantee all of the c components are freed if either vectoring doesn't
     * execute or this sink was not added to any relays.  Sink the pointer is
     * zeroed out inside of sinkRaze, this function can be called twice.
     * Furthermore, since this function is called explicitly, java calls the 
     * c component rather than vice-versa.
     */
    public void raze()
    {
        if ( pointer != 0 )
            raze( pointer );
        
        pointer = 0;
    }

    /* 
     * This function is called by vectoring once it completes.  The C component is 
     * freed automatically.
     */
    protected void sinkRaze()
    {
        /* NULL out the pointer */
        pointer = 0;        
    }

    protected native void raze( int pointer );

    static
    {
        Vector.load();
    }
}
