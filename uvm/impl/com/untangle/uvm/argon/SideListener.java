/**
 * $Id$
 */
package com.untangle.uvm.argon;

import com.untangle.jvector.Sink;
import com.untangle.jvector.SinkEndpointListener;
import com.untangle.jvector.Source;
import com.untangle.jvector.SourceEndpointListener;

class SideListener implements SinkEndpointListener, SourceEndpointListener
{
    protected long txChunks = 0;
    protected long txBytes  = 0;

    protected long rxChunks = 0;
    protected long rxBytes  = 0;

    protected boolean isSourceShutdown = false;
    protected boolean isSinkShutdown   = false;

    protected SideListener()
    {
    }
    
    public void dataEvent( Source source, int numBytes )
    {
        rxChunks++;
        rxBytes += numBytes;
    }
    
    public void dataEvent( Sink sink, int numBytes )
    {
        txChunks++;
        txBytes += numBytes;
    }
    
    public void shutdownEvent( Source source )
    {
        isSourceShutdown = true;
    }
    
    public void shutdownEvent( Sink sink )
    {
        isSinkShutdown   = true;
    }

    protected boolean isShutdown()
    {
        return isSinkShutdown & isSourceShutdown;
    }

    protected String stats()
    {
        return "rx: " + rxBytes + "/" + rxChunks + " tx: " + txBytes + "/" + txChunks;
    
    }
}
