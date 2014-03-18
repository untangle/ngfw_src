/**
 * $Id$
 */
package com.untangle.jvector;

public abstract class Source
{
    protected long pointer;

    /* ??? Should this be package protected */
    protected long src_ptr()
    {
        return pointer;
    }

    protected abstract Crumb get_event( Sink snk );

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

    public boolean isRazed()
    {
        if ( pointer == 0L )
            return true;
        else
            return false;
    }

    /* 
     * This function is called by vectoring once it completes.  The C component is 
     * freed automatically.  Since this is called from C, the C component is freed
     * automatically
     */
    protected void sourceRaze()
    {
        pointer = 0;
    }
    
    protected native void raze( long pointer );

    static 
    {
        Vector.load();
    }
}
