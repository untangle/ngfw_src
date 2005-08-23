package com.metavize.jvector;

import java.util.LinkedList;

public abstract class SocketQueueImpl implements SocketQueue
{
    protected boolean containsReset = false;
    protected boolean containsShutdown = false;
    protected int mvpollKey = 0;
    
    protected native int mvpoll_key_notify_observers( int pointer, int state);

    /**
     * The event list
     */
    /* XXXChange to a generic */
    protected LinkedList eventList;

    /**
     * max length of the eventList
     * aside from special events, which can be appended regardless
     */
    private int maxEvents = 1;

    /**
     * The listeners of this queue
     * In the case of incoming queue, they are called when
     * something new comes in,
     * In the case of outgoing queue, they are called when
     * it is empty
     */
    protected LinkedList listeners;

    /**
     * The attachment
     */
    private Object attachment = null;

    /**
     * The mvpoll key
     */
    public SocketQueueImpl( int pointer )
    {
        mvpollKey = pointer;
        eventList = new LinkedList();
        listeners = new LinkedList();
    }
    
    public boolean isEmpty ()
    {
        return eventList.isEmpty();
    }

    public boolean isFull ()
    {
        if (eventList.size() >= this.maxEvents)
            return true;
        else
            return false;
    }

    public int 	numEvents ()
    {
        return eventList.size();
    }

    public void maxEvents( int n )
    {
        this.maxEvents = n;
    }

    public void attach (Object o)
    {
        this.attachment = o;
    }

    public Object attachment ()
    {
        return this.attachment;
    }

    /**
     * Register a listener for the socket queue.
     * @return True if the listener was added, false otherwise.
     */
    public boolean registerListener( SocketQueueListener l )
    {
        return listeners.add(l);
    }

    public boolean unregisterListener( SocketQueueListener l)
    {
        return listeners.remove(l);
    }
    
    public boolean add( Crumb crumb )
    {
        switch ( crumb.type()) {
        case Crumb.TYPE_RESET:
            containsShutdown = true;
            containsReset = true;

            /* Put reset crumbs on the front */
            eventList.addFirst( crumb );
            break;

        case Crumb.TYPE_SHUTDOWN: 
            containsShutdown = true;
            
            /* fallthrough */
        default:
            eventList.addLast( crumb );
        }

        callListenersAdd( crumb );
        return true;
    }

    public Crumb removeFirst() {
        Crumb crumb = (Crumb)eventList.removeFirst();
        callListenersRemove();
        return crumb;
    }

    public boolean containsReset()
    {
        return containsReset;
    }

    public boolean containsShutdown()
    {
        return containsShutdown;
    }

    protected void notifyMvpoll()
    {
        mvpoll_key_notify_observers( mvpollKey, poll());
    }

    public abstract int poll();

    protected abstract void callListenersAdd( Crumb crumb );
    protected abstract void callListenersRemove();
}
