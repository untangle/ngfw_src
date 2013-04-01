/**
 * $Id$
 */
package com.untangle.uvm.netcap;

import com.untangle.jvector.Sink;
import com.untangle.jvector.SinkEndpointListener;
import com.untangle.jvector.Source;
import com.untangle.jvector.SourceEndpointListener;

public class SideListener implements SinkEndpointListener, SourceEndpointListener
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

    public long getRxBytes() { return this.rxBytes; }
    public long getTxBytes() { return this.txBytes; }
    public long getRxChunks() { return this.rxChunks; }
    public long getTxChunks() { return this.txChunks; }
    
    public boolean isShutdown()
    {
        return isSinkShutdown & isSourceShutdown;
    }

    public String stats()
    {
        return "rx: " + rxBytes + "/" + rxChunks + " tx: " + txBytes + "/" + txChunks;
    
    }
}
