/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SocketQueue.java,v 1.7 2005/01/13 19:47:50 rbscott Exp $
 */

package com.metavize.jvector;

public interface SocketQueue
{   
    public boolean isEmpty ();

    public boolean isFull ();

    public int 	numEvents ();

    public boolean containsShutdown();
    
    public boolean containsReset();
    
    /* ??? Does this need to be in the SocketQueue interface */
    public void maxEvents( int n );

    public void attach ( Object o );

    public boolean add( Crumb crumb );

    public Object attachment ();

    /**
     * Register a listener for the socket queue.
     * @return True if the listener was added, false otherwise.
     */
    public boolean registerListener( SocketQueueListener l );

    /**
     * Register a listener for the socket queue.
     * @return True if the listener was removed, false otherwise.
     */
    public boolean unregisterListener ( SocketQueueListener l );
    
    public int poll();
}
