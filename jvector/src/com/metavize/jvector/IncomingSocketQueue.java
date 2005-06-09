/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.jvector;

import java.util.ListIterator;

public class IncomingSocketQueue extends Sink implements SocketQueue
{
    protected final ISocketQueue sq;

    /**
     * This is a hook that is called after reading a shutdown crumb
     */
    protected SocketQueueShutdownHook shutdownHook = null;

    protected boolean isClosed = false;

    // True once the session is killed
    protected boolean isKilled = false;
    
    // True once the session has been reset
    protected boolean isReset  = false;
        
    public IncomingSocketQueue()
    {
        pointer = create();
        sq = new ISocketQueue( mvpollKey( pointer ));
    }

    /* Kind of no longer needed since IncomingSocketQueue implemts SocketQueue */
    public SocketQueue sq()
    {
        return sq;
    }

    public Crumb read()
    {
        if ( isReset ) {
            /* Return a reset crumb */
            return ResetCrumb.getInstance();
        }

        Crumb crumb = (Crumb)sq.removeFirst();
        
        if ( Vector.isDebugEnabled()) {
            if ( crumb.isData()) {
                Vector.logDebug( "Read data crumb(" + this + "): " + crumb + 
                                 ", limit: " + ((DataCrumb)crumb).limit());
            } else {
                Vector.logDebug( "Read crumb(" + this + "): " + crumb );
            }
        }
        
        /* Once the client reads the shutdown crumb, the socket queue is shutdown */
        if  ( crumb.isShutdown()) isClosed = true;

        /* XXX, not sure if this is used. Call the shutdown hook */
        if ( shutdownHook != null ) shutdownHook.shutdownEvent( this );

        return crumb;
    }
    
    /** 
     * This is to get a crumb without taking it out of the buffer, you must call read 
     * afterwards to remove the crumb.
     */
    public Crumb peek()
    {
        return (Crumb)sq.eventList.getFirst();
    }

    /**
     * This is from the Listeners standpoint, this is a way to close the input on
     * a sink.  This is done when a listener no longer wants to receive data from the
     * socket queue.  EG. when the corresponding incoming socket queue has been reset.
     */
    public void reset()
    {
        if ( Vector.isDebugEnabled())
            Vector.logDebug( "reset(" + this + ")" );

        /* Clear everything in the event list */
        isReset = true;
        sq.eventList.clear();
        sq.isShutdown = true;
        sq.notifyMvpoll();
    }
    
    /**
     * Check to see if input is closed, an incoming socket queue is
     * considered closed once a shutdown crumb of any type is read.
     */
    public boolean isClosed()
    {
        return isClosed;
    }

    /**
     * Check to see if read events are enabled on this incoming socket queue
     */
    public boolean isEnabled()
    {
        return sq.isEnabled;
    }

    /**
     * Disable read events on this incoming socket queue
     */
    public void disable()
    {
        sq.isEnabled = false;
        sq.notifyMvpoll();
    }
    
    /**
     * Enable read events on this incoming socket queue
     */
    public void enable()
    {
        sq.isEnabled = true;
        sq.notifyMvpoll();
    }
    
    protected int send_event( Crumb crumb )
    {
        sq.add( crumb );

        if ( crumb.isShutdown()) {
            sq.eventList.clear();
            sq.callListenersRemove();
            return Vector.ACTION_SHUTDOWN;
        }

        if ( !sq.isEmpty()) {
            if ( Vector.isDebugEnabled()) {
                if ( crumb.isData()) {
                    DataCrumb dc = (DataCrumb)crumb;
                    Vector.logDebug( "Put data crumb " + crumb + "(" + dc.offset() + "," + dc.limit() + 
                                     ") back in relay from IncomingSocketQueue: " + this +
                                     " queue size:" + sq.eventList.size());
                } else {
                    Vector.logDebug( "Put non-data crumb " + crumb +
                                     "back in relay from IncomingSocketQueue: " + this +
                                     " queue size:" + sq.eventList.size());
                }
            }

            sq.removeFirst();
            return Vector.ACTION_NOTHING;
        }
        
        return Vector.ACTION_DEQUEUE;
    }

    protected int shutdown() 
    {
        /**
         * Have to notify the transform that you are shutting down, only if you haven't
         * notified them yet, this is at expiration time, the session is dead,
         * and you must use an expired session crumb 
         */
        if ( !isClosed ) sq.add( ShutdownCrumb.getInstanceExpired());

        return 0;
    }
    
    /**
     * Kill this incoming socket queue, this should really only be used for error conditions.
     */
    public void kill()
    {
        isClosed = true;
        sq.isShutdown = true;
        isKilled = true;
        
        /* Deregister all listeners */
        sq.listeners.clear();
        
        sq.notifyMvpoll();
    }

    protected native int create();
    protected static native int mvpollKey( int pointer );

    public boolean isEmpty () { return sq.isEmpty(); } 

    public boolean isFull () { return sq.isFull(); }

    public boolean containsReset() { return sq.containsReset(); }

    public boolean containsShutdown() { return sq.containsShutdown(); }

    public int 	numEvents () { return sq.numEvents(); }

    /* ??? Does this need to be in the SocketQueue interface */
    public void maxEvents( int n ) { sq.maxEvents( n ); }

    public void attach ( Object o ) { sq.attach( o ); }

    public boolean add( Crumb crumb ) { return sq.add( crumb ); }

    public Object attachment () { return sq.attachment(); }

    public boolean registerListener( SocketQueueListener l ) { return sq.registerListener( l ); }

    public boolean unregisterListener ( SocketQueueListener l ) { return sq.unregisterListener( l ); }
    
    public int poll() { return sq.poll(); }

    private class ISocketQueue extends SocketQueueImpl
    {
        protected boolean isEnabled  = true;
        /* Once an Incoming Socket queue is shutdown, poll always returns
         * MVPOLL_HUP */
        protected boolean isShutdown = false;
        
        protected ISocketQueue( int mvpollKey )
        {
            super( mvpollKey );
        }

        public int poll()
        {
            int mask = 0;
            
            if ( isShutdown ) return MVPOLLHUP;
            
            if ( isEnabled && !this.isFull())
                mask |= MVPOLLOUT;
            
            return mask;
        }
        
        protected void callListenersAdd( Crumb crumb )
        {
            int poll;
            boolean wasShutdown = isShutdown;

            if ( crumb.isShutdown() ) isShutdown = true;
            
            notifyMvpoll();
            
            if ( wasShutdown ) {
                /* Clear out all of the events */
                eventList.clear();
            } else {
                for (ListIterator iter = this.listeners.listIterator() ; iter.hasNext() ;) {
                    SocketQueueListener ll = (SocketQueueListener) iter.next();
                    ll.event( IncomingSocketQueue.this );
                    /* If an exception killed the thread, don't finish iterating the list */
                    if ( isKilled ) break;
                }
            }
        }

        protected void callListenersRemove()
        {
            notifyMvpoll();
        }
    }
}

