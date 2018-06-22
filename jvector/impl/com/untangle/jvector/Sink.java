/**
 * $Id$
 */
package com.untangle.jvector;

/**
 * Sink
 */
public abstract class Sink
{
    protected long pointer;

    /**
     * snk_ptr gets the sink pointer
     * @return 
     */
    protected long snk_ptr() 
    { 
        return pointer;
    }

    /**
     * send_event
     * @param o
     * @return
     */
    protected abstract int send_event(Crumb o);

    /**
     * shutdown
     * @return
     */
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
        if ( pointer != 0L )
            raze( pointer );
        
        pointer = 0L;
    }

    /**
     * isRazed - true if razed, false otherwise
     * @return bool
     */
    public boolean isRazed()
    {
        if ( pointer == 0L )
            return true;
        else
            return false;
    }
    
    /**
     * This function is called by vectoring once it completes.  The C component is 
     * freed automatically.
     */
    protected void sinkRaze()
    {
        /* NULL out the pointer */
        pointer = 0L;        
    }

    /**
     * raze - raze the sink
     * @param pointer
     */
    protected native void raze( long pointer );

    static
    {
        Vector.load();
    }
}
