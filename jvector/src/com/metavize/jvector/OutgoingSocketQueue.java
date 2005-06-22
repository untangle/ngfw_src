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

public class OutgoingSocketQueue extends Source implements SocketQueue
{
    protected final OSocketQueue sq;
    /**
     * This is a hook that is called after writing a shutdown crumb
     */
    protected SocketQueueShutdownHook shutdownHook = null;

    /**
     *  Closed relative to the Listeners
     */
    protected boolean isListenersSideClosed = false;

    /**
     * Closed relative to the relay, this occurs when a shutdown is read from the
     * relay
     */
    protected boolean isRelaySideClosed = false;
    
    public OutgoingSocketQueue()
    {
        pointer = create();
        sq = new OSocketQueue( mvpollKey( pointer ));
    }

    /* Kind of no longer needed since IncomingSocketQueue implements SocketQueue */
    public SocketQueue sq()
    {
        return sq;
    }

    protected Crumb get_event()
    {
        if ( sq.isEmpty()) {
            Vector.logError( "get_event without any data in the socket queue" );

            if ( isRelaySideClosed ) return ShutdownCrumb.getInstance();
                
            throw new IllegalStateException( "get_event without any data in the socket queue" );
        }

        Crumb crumb = (Crumb)sq.removeFirst();
        
        if ( Vector.isDebugEnabled()) {
            if ( crumb.isData()) {
                Vector.logDebug( "get_event data crumb(" + this + "): " + crumb + 
                                 ", limit: " + ((DataCrumb)crumb).limit());
            } else {
                Vector.logDebug( "get_event crumb(" + this + "): " + crumb );
            }
        }

        if ( crumb.isShutdown()) {
            isRelaySideClosed = true;
            sq.notifyMvpoll();
        }

        return crumb;
    }

    /**
     * Write a crumb to that is destined for the relay.</p>
     * @param crumb - The crumb to write.
     * @returns true if the crumb was written, false otherwise.
     */
    public boolean write( Crumb crumb )
    {
        if ( Vector.isDebugEnabled()) {
            if ( crumb.isData()) {
                Vector.logDebug( "Write data crumb(" + this + "): " + crumb + 
                                 ", limit: " + ((DataCrumb)crumb).limit());
            } else {
                Vector.logDebug( "Write crumb(" + this + "): " + crumb );
            }
        }
        
        if ( crumb.isShutdown()) isListenersSideClosed = true;

        return sq.add( crumb );
    }
   
    /** 
     * Check to see if the listeners side of the Outgoing Socket Queue is closed.
     * This is true once the transform has written a shutdown crumb into the 
     * outgoing socket queue.
     */
    public boolean isClosed()
    {
        return isListenersSideClosed;
    }

    /**
     * Check to see if writable events are enabled on this outgoing socket queue.  Write events
     * are edge triggered, so it doesn't make a lot of sense toe enable or disable them.
     */
    public boolean isEnabled()
    {
        return sq.isEnabled;
    }

    /**
     * Disable write events on this outgoing socket queue
     */
    public void disable()
    {
        sq.isEnabled = false;
        sq.notifyMvpoll();
    }

    /**
     * Enable write events on this outgoing socket queue
     */
    public void enable()
    {
        sq.isEnabled = true;
        sq.notifyMvpoll();
    }

    /**
     * Kill this outgoing socket queue, this should only be used for error conditions.
     */
    public void kill()
    {
        isListenersSideClosed = true;

        /* Clear all of the elements in the event list */
        sq.eventList.clear();

        /* Send a reset crumb down the line */
        if ( !isRelaySideClosed ) {
            sq.add( ResetCrumb.getInstance());
        }
    }

    /**
     * Register a hook to be called after writing a shutdown crumb.
     */
    public void registerShutdownHook( SocketQueueShutdownHook hook )
    {
        shutdownHook = hook; 
    }

    protected int shutdown() {
        isRelaySideClosed     = true;

        /* Clear all of the elements in the event list */
        sq.eventList.clear();

        /* Call the shutdown hook */
        for (ListIterator iter = sq.listeners.listIterator() ; iter.hasNext() ;) {
            SocketQueueListener ll = (SocketQueueListener) iter.next();
            ll.shutdownEvent( OutgoingSocketQueue.this );
        }

        return 0;
    }
    
    protected native int create();
    protected static native int mvpollKey( int pointer );

    public boolean isEmpty () { return sq.isEmpty(); } 

    public boolean isFull () { return sq.isFull(); }

    public boolean containsReset() { return sq.containsReset(); }

    public boolean containsShutdown() { return sq.containsShutdown(); }

    public int 	numEvents () { return sq.numEvents(); }

    /**
     * XXX This should go away
     */
    public int numBytes() 
    { 
        return -1;
    }

    /* ??? Does this need to be in the SocketQueue interface */
    public void maxEvents( int n ) { sq.maxEvents( n ); }

    public void attach ( Object o ) { sq.attach( o ); }

    public boolean add( Crumb crumb ) { return sq.add( crumb ); }

    public Object attachment () { return sq.attachment(); }

    public boolean registerListener( SocketQueueListener l ) { return sq.registerListener( l ); }

    public boolean unregisterListener ( SocketQueueListener l ) { return sq.unregisterListener( l ); }

    public int poll() { return sq.poll(); }

    private class OSocketQueue extends SocketQueueImpl
    {
        protected boolean isEnabled = true;
        
        protected OSocketQueue( int mvpollKey )
        {
            super( mvpollKey );
        }

        public int poll()
        {
            /* If the relay side is closed, always return HUP */
            if ( isRelaySideClosed ) return MVPOLLHUP;
            
            if ( containsShutdown ) {
                /* containsShutdown, doesn't matter if it is enabled, already shutdown */

                /* If the event list is empty (shutdown already read), or the next event is
                 * shutdown, return a HUP */
                if ( eventList.isEmpty() || ((Crumb)(eventList.getFirst())).isShutdown()) {
                    return MVPOLLHUP;
                }
                
                /* More events, but not a shutdown */
                return MVPOLLIN;
            }

            /* If enabled, and there are more events, then this is writable */
            if ( isEnabled && !eventList.isEmpty())
                return MVPOLLIN;
            
            /* No events are ready right now */
            return 0;
        }
        
        protected void callListenersAdd( Crumb crumb ) /* call Listeners Non Empty (readable) Event */
        {
            /* Relay side was closed, but the transform is not aware yet */
            if ( isRelaySideClosed ) {
                /* Drop the buffer */
                eventList.clear();
            } else {
                notifyMvpoll();
            }
        }

        protected void callListenersRemove() /* call Listeners Non Full (writable) Event */
        {
            /**
             * if it is still full after removing, neither mvpoll nor
             * the listeners need to know because the state hasnt
             * changed (its still not writable)
             **/
            if (isFull()) 
                return;
            
            notifyMvpoll();

            /** Only call the listeners if there side is open */
            if ( !isListenersSideClosed ) {
                for (ListIterator iter = this.listeners.listIterator() ; iter.hasNext() ;) {
                    SocketQueueListener ll = (SocketQueueListener) iter.next();
                    ll.event( OutgoingSocketQueue.this );
                }
            }
        }
    }
}
