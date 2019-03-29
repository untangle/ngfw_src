/**
 * $Id$
 */
package com.untangle.jvector;

import java.util.LinkedList;
import java.util.List;

/**
 * IncomingSocketQueue:
 * This is not actually a queue, it is a way to hand off events(crumbs)
 * from the relay to any of the listeners.  This object doesn't actually hold any data
 * in its normal state, and there is no notion of "leaving" things inside of a incoming
 * socket queue.  What this means, is at the end of each event, the crumb is either taken
 * by the listener and removed from the relay, or it is left in the relay at which point
 * a new event will occur with the same crumb.
 */
public class IncomingSocketQueue extends Sink
{
    private Object attachment;

    /* True if the listeners want events */
    private boolean isEnabled = true;

    /* True once the session is shutdown.
     *  a. A read event was delivered to a listener where the crumb was a shutdown event.
     *     This will shutdown the IncomingSocketQueue regardless of whether the listener actually
     *     reads the event.
     *  b. A transform explicitly kills or resets an incoming socket queue
     */
    private boolean isShutdown = false;

    /* This seems very redundant of isShutdown, but it is not.  The IncomingSocketQueue contains
     * a shutdown after the first time vectoring sends a shutdown event, but isShutdown is false
     * until after the readEvent is delivered to the listeners
     */
    private boolean containsShutdown = false;

    /* True once the session is killed A kill is an explicit event
     * caused by an error during a read event, after killing an
     * IncomingSocketQueue, iteration of the listeners is stopped
     */
    private boolean isKilled = false;

    /* True once a reset has been occured. */
    private boolean isReset  = false;

    /* XXXX This seems very redundant of isReset */
    private boolean containsReset = false;

    private boolean containsTimeout = false;

    // Current crumb to read out
    private Crumb currentCrumb;

    private String debugString = "";
    
    // List of listeners
    private final List<SocketQueueListener> listenerList = new LinkedList<>();

    /**
     * IncomingSocketQueue
     * @param debugString
     */
    public IncomingSocketQueue( String debugString )
    {
        /* The pointer is from the sink */
        this.pointer = create();
        this.debugString = debugString;
    }

    /**
     * read - reads a crumb
     * @return - the crumb
     */
    public Crumb read()
    {
        if ( isReset ) {
            currentCrumb = null;

            /* Return a reset crumb */
            return ResetCrumb.getInstance();
        }

        /* XXX This should throw an exception if there isn't anything in there */
        Crumb crumb = currentCrumb;

        /* Null out the current crumb to indicate that the data has been read */
        currentCrumb = null;

        if ( Vector.isDebugEnabled()) {
            if ( crumb.isData()) {
                Vector.logDebug( "jvector: [" + debugString + "] " + this + ": read " + ((DataCrumb)crumb).limit() + " bytes." );
            } else {
                Vector.logDebug( "jvector: [" + debugString + "] " + this + ": read crumb " + crumb.getClass() + "." );
            }
        }

        /* Once the client reads the shutdown crumb, the socket queue is closed */
        if  ( crumb.isShutdown()) isShutdown = true;

        return crumb;
    }

    /**
     * send_event
     * @param crumb
     * @return number sent
     */
    @SuppressWarnings("fallthrough")
    public int send_event( Crumb crumb )
    {
        boolean wasShutdown = isShutdown;

        switch ( crumb.type()) {
        case Crumb.TYPE_RESET:
            this.containsReset = true;
            /* ???? Should the isResetFlag be set here */

            /* Fallthrough */
        case Crumb.TYPE_SHUTDOWN:
            this.containsShutdown = true;
        }

        mvpollNotifyObservers();

        /* Nothing to do */
        if ( wasShutdown ) return Vector.ACTION_SHUTDOWN;

        this.currentCrumb = crumb;

        for ( SocketQueueListener listener : this.listenerList ) {
            listener.event( this );

            /* If an exception killed the thread, don't finish iterating the list */
            if ( isKilled ) break;
        }

        if ( crumb.isShutdown() || this.isShutdown ) {
            /* Closing time */
            this.isShutdown  = true;
            this.currentCrumb = null;
            mvpollNotifyObservers();
            return Vector.ACTION_SHUTDOWN;
        }

        /* This determines whether to dequeue from relay, or to leave the crumb in the relay */
        if ( currentCrumb == null ) {
            /* Crumb was read out, it is a listeners responsibility now, dequeue it from the relay */
            return Vector.ACTION_DEQUEUE;
        }

        /* Crumb hasn't been processed yet, it must be stay inside of the relay */
        this.currentCrumb = null;
        return Vector.ACTION_NOTHING;
    }

    /**
     * somewhat deprecated, but still used
     * @return the number of events
     */
    public int numEvents()
    {
        return ( currentCrumb == null ) ? 1 : 0;
    }

    /**
     * This is to get a crumb without taking it out of the buffer, you must call read
     * afterwards to remove the crumb.
     * @return the crumb
     */
    public Crumb peek()
    {
        return currentCrumb;
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
        isReset      = true;
        currentCrumb = null;
        isShutdown   = true;
        mvpollNotifyObservers();
    }

    /**
     * Check to see if input is closed, an incoming socket queue is
     * considered closed once a shutdown crumb of any type is read.
     * @return true if closed, false otherwise
     */
    public boolean isClosed()
    {
        return isShutdown;
    }

    /**
     * Check to see if read events are enabled on this incoming socket queue
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled()
    {
        return isEnabled;
    }

    /**
     * Disable read events on this incoming socket queue
     */
    public void disable()
    {
        isEnabled = false;
        mvpollNotifyObservers();
    }

    /**
     * Enable read events on this incoming socket queue
     */
    public void enable()
    {
        isEnabled = true;
        mvpollNotifyObservers();
    }

    /**
     * shutdown
     * @return
     */
    protected int shutdown()
    {
        /**
         * Have to notify the transform that you are shutting down, only if you haven't
         * notified them yet, this is at expiration time, the session is dead,
         * and you must use an expired session crumb
         */
        if ( !isShutdown ) {
            send_event( ShutdownCrumb.getInstanceExpired());
        }

        return 0;
    }

    /**
     * Kill this incoming socket queue, this should really only be used for error conditions.
     */
    public void kill()
    {
        this.isShutdown = true;
        this.isKilled   = true;

        /* Deregister all listeners */
        this.currentCrumb = null;

        mvpollNotifyObservers();
    }

    /**
     * isEmpty
     * @return
     */
    public boolean isEmpty()
    {
        return ( this.currentCrumb == null );
    }

    /**
     * isFull
     * @return
     */
    public boolean isFull()
    {
        return ( this.currentCrumb != null );
    }

    /**
     * containsReset
     * @return
     */
    public boolean containsReset()
    {
        return this.containsReset;
    }

    /**
     * containsShutdown
     * @return
     */
    public boolean containsShutdown()
    {
        return containsShutdown;
    }

    /**
     * containsTimeout
     * @return
     */
    public boolean containsTimeout()
    {
        return containsTimeout;
    }

    /**
     * attach
     * @param o
     */
    public void attach( Object o )
    {
        this.attachment = o;
    }

    /**
     * attachment
     * @return
     */
    public Object attachment()
    {
        return this.attachment;
    }

    /**
     * registerListener
     * @param l
     * @return
     */
    public boolean registerListener( SocketQueueListener l )
    {
        return this.listenerList.add( l );
    }

    /**
     * poll
     * @return
     */
    public int poll()
    {
        if ( isShutdown ) return Vector.MVPOLLHUP;
        if ( isEnabled )  return Vector.MVPOLLOUT;

        return 0;
    }

    /**
     * mvpollNotifyObservers
     */
    private void mvpollNotifyObservers()
    {
        if ( this.pointer != 0L )
            mvpollNotifyObservers( this.pointer, poll() );
    }

    /**
     * create
     * @return
     */
    private native long create();

    /**
     * mvpollNotifyObservers
     * @param pointer
     * @param eventMask
     */
    private native void mvpollNotifyObservers( long pointer, int eventMask );
}

