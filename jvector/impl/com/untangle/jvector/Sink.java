/**
 * $Id: Sink.java 35595 2013-08-12 21:44:12Z dmorris $
 */
package com.untangle.jvector;

public abstract class Sink
{
    protected long pointer;

    protected long snk_ptr() 
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
        if ( pointer != 0L )
            raze( pointer );
        
        pointer = 0L;
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

    protected native void raze( long pointer );

    static
    {
        Vector.load();
    }
}
