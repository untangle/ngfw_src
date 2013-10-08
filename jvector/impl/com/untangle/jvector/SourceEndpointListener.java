/**
 * $Id: SourceEndpointListener.java 35567 2013-08-08 07:47:12Z dmorris $
 */
package com.untangle.jvector;

public interface SourceEndpointListener
{
    /**
     * An event that occurs each time the endpoint receives data.
     * @param source - The source that triggered the event.
     * @param numBytes - The number of bytes transmitted.
     */
    void dataEvent( Source source, int numBytes );


    /**
     * An event that occurs when the source or sink shuts down
     * @param source - The source that triggered the event.
     */
    void shutdownEvent( Source source );
}
