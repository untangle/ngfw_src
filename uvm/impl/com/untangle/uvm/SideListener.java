/**
 * $Id$
 */
package com.untangle.uvm;

import com.untangle.jvector.Sink;
import com.untangle.jvector.SinkEndpointListener;
import com.untangle.jvector.Source;
import com.untangle.jvector.SourceEndpointListener;

/**
 * SideListener is just a listener object that listens to a side and tracks stats
 */
public class SideListener implements SinkEndpointListener, SourceEndpointListener
{
    protected long txChunks = 0;
    protected long txBytes  = 0;

    protected long rxChunks = 0;
    protected long rxBytes  = 0;

    protected boolean isSourceShutdown = false;
    protected boolean isSinkShutdown   = false;

    /**
     * SideListener constructor
     */
    protected SideListener() { }
    
    /**
     * dataEvent - called when a data event occurs
     * @param source
     * @param numBytes
     */
    public void dataEvent( Source source, int numBytes )
    {
        rxChunks++;
        rxBytes += numBytes;
    }
    
    /**
     * dataEvent - called when a data event occurs
     * @param sink
     * @param numBytes
     */
    public void dataEvent( Sink sink, int numBytes )
    {
        txChunks++;
        txBytes += numBytes;
    }
    
    /**
     * shutdownEvent - called when a shutdown event occurs
     * @param source
     */
    public void shutdownEvent( Source source )
    {
        isSourceShutdown = true;
    }
    
    /**
     * shutdownEvent - called when a shutdown event occurs
     * @param sink
     */
    public void shutdownEvent( Sink sink )
    {
        isSinkShutdown   = true;
    }

    /**
     * getRxBytes
     * @return count
     */
    public long getRxBytes()
    {
        return this.rxBytes;
    }

    /**
     * getTxBytes
     * @return count
     */
    public long getTxBytes()
    {
        return this.txBytes;
    }

    /**
     * getRxChunks
     * @return count
     */
    public long getRxChunks()
    {
        return this.rxChunks;
    }

    /**
     * getTxChunks
     * @return count
     */
    public long getTxChunks()
    {
        return this.txChunks;
    }
    
    /**
     * isShutdown returns true if is shutdown or false otherwise
     * @return bool
     */
    public boolean isShutdown()
    {
        return isSinkShutdown & isSourceShutdown;
    }

    /**
     * stats - returns a string representation of the stats
     * @return String
     */
    public String stats()
    {
        return "rx: " + rxBytes + "/" + rxChunks + " tx: " + txBytes + "/" + txChunks;
    
    }
}
