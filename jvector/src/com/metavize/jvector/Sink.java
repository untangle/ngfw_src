/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Sink.java,v 1.5 2005/01/30 00:07:38 rbscott Exp $
 */

package com.metavize.jvector;

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
