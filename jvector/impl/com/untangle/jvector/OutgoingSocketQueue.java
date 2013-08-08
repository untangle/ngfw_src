/**
 * $Id$
 */
package com.untangle.jvector;

import java.util.LinkedList;
import java.util.List;

public class OutgoingSocketQueue extends Source
{
    /**
     * List of events to go out
     */
    private final List<Crumb> eventList = new LinkedList<Crumb>();

    /**
     * List of listeners
     */
    private final List<SocketQueueListener> listenerList = new LinkedList<SocketQueueListener>();

    /**
     * The attachment
     */
    private Object attachment = null;

    /**
     *  Closed relative to the listeners
     */
    private boolean isListenersSideClosed = false;

    /**
     * Closed relative to the relay, this occurs when a shutdown is read from the
     * relay
     */
    private boolean isRelaySideClosed = false;

    private boolean isEnabled             = false;
    private boolean containsReset         = false;
    private boolean containsShutdown      = false;

    /* Max events is the capacity of the outgoing socket queue */
    private int maxEvents = 1;

    public OutgoingSocketQueue()
    {
        pointer = create();
    }

    protected Crumb get_event()
    {
        if ( this.eventList.isEmpty()) {
            Vector.logError( "get_event without any data in the OutgoingSocketQueue" );

            if ( isRelaySideClosed ) return ShutdownCrumb.getInstance();

            throw new IllegalStateException( "get_event without any data in the socket queue" );
        }

        Crumb crumb = eventList.remove( 0 );
        callListenersRemove();

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
            mvpollNotifyObservers();
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
        if ( crumb.isShutdown()) {
            isListenersSideClosed = true;
        } else if ( isFull()) {
            Vector.logError( "Adding non-shutdown crumb to full socket" );
        }

        return add( crumb );
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
        return isEnabled;
    }

    /**
     * Disable write events on this outgoing socket queue
     */
    public void disable()
    {
        isEnabled = false;
        mvpollNotifyObservers();
    }

    /**
     * Enable write events on this outgoing socket queue
     */
    public void enable()
    {
        isEnabled = true;
        mvpollNotifyObservers();
    }

    /**
     * Kill this outgoing socket queue, this should only be used for error conditions.
     */
    public void kill()
    {
        isListenersSideClosed = true;

        /* Clear all of the elements in the event list */
        this.eventList.clear();

        /* Send a reset crumb down the line */
        if ( !isRelaySideClosed ) {
            add( ResetCrumb.getInstance());
        }
    }

    protected int shutdown()
    {
        isRelaySideClosed     = true;

        /* Clear all of the elements in the event list */
        this.eventList.clear();

        /* Call the shutdown hook */

        for ( SocketQueueListener listener : this.listenerList ) {
            listener.shutdownEvent( this );
        }

        return 0;
    }

    public boolean isEmpty()
    {
        return this.eventList.isEmpty();
    }

    public boolean isFull()
    {
        return ( this.eventList.size() >= maxEvents );
    }

    public boolean containsReset()
    {
        return this.containsReset;
    }

    public boolean containsShutdown()
    {
        return this.containsShutdown;
    }

    public int  numEvents()
    {
        return eventList.size();
    }

    public int numBytes()
    {
        return -1;
    }

    public void maxEvents( int maxEvents )
    {
        this.maxEvents = maxEvents;
    }

    public void attach( Object o )
    {
        this.attachment = o;
    }

    public Object attachment() {
        return this.attachment;
    }

    @SuppressWarnings("fallthrough")
    public boolean add( Crumb crumb )
    {
        switch( crumb.type()) {
        case Crumb.TYPE_RESET:
            containsShutdown = true;
            containsReset   = true;
            eventList.add( 0, crumb );
            break;

        case Crumb.TYPE_SHUTDOWN:
            containsShutdown = true;

            /* Fallthrough */
        default:
            eventList.add( crumb );
        }

        /* Relay side was closed, but the transform is not aware yet */
        if ( isRelaySideClosed ) {
            /* Drop the buffer */
            eventList.clear();
        } else {
            mvpollNotifyObservers();
        }

        return true;
    }

    public boolean registerListener( SocketQueueListener l )
    {
        return this.listenerList.add( l );
    }

    // public boolean unregisterListener( SocketQueueListener l )
    // {
    //     return this.listenerList.remove( l );
    // }

    public int poll()
    {
        /* If the relay side is closed, always return HUP */
        if ( isRelaySideClosed ) return Vector.MVPOLLHUP;

        if ( containsShutdown ) {
            /* containsShutdown, doesn't matter if it is enabled, already shutdown */

            /* If the event list is empty (shutdown already read), or the next event is
             * shutdown, return a HUP */
            if ( eventList.isEmpty() || (eventList.get( 0 )).isShutdown()) {
                return Vector.MVPOLLHUP;
            }

            /* More events, but not a shutdown */
            return Vector.MVPOLLIN;
        }

        /* If enabled, and there are more events, then this is readable */
        if ( isEnabled && !eventList.isEmpty())
            return Vector.MVPOLLIN;

        /* No events are ready right now */
        return 0;
    }

    private void mvpollNotifyObservers()
    {
        mvpollNotifyObservers( this.pointer, poll());
    }

    private void callListenersRemove() /* call Listeners Non Full (writable) Event */
    {
        /**
         * if it is still full after removing, neither mvpoll nor
         * the listeners need to know because the state hasnt
         * changed (its still not writable)
         **/
        if ( isFull()) return;

        mvpollNotifyObservers();

        /** Only call the listeners if there side is open */
        if ( !isListenersSideClosed ) {
            for ( SocketQueueListener listener : this.listenerList ) {
                listener.event( OutgoingSocketQueue.this );
            }
        }
    }

    private native long create();
    private native void mvpollNotifyObservers( long pointer, int eventMask );
}
