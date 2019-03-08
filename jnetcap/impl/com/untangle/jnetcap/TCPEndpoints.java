/**
 * $Id$
 */
package com.untangle.jnetcap;

/**
 * Interface for TCPEndpoints
 */
public interface TCPEndpoints extends Endpoints
{
    /**
     * Get the file description.
     * @return FD
     */
    public int fd();

    /**
     * Configure a TCP File descriptor for blocking or non-blocking mode.<p/>
     *
     * @param mode <code>true</code> enable blocking, <code>false</code> to disable blocking.
     */
    public void blocking( boolean mode );

    /**
     * read from this TCP endpoint
     * @param data
     * @return the number of bytes
     */
    public int read( byte[] data );

    /**
     * write data to this TCP endpoint
     * @param data
     * @return the number of written bytes
     */
    public int write( byte[] data );

    /**
     * write the string to this TCP endpoint
     * @param data <doc>
     * @return the number of written bytes
     */
    public int write( String data );

    /**
     * close this endpoint (FD)
     */
    public void close();
}
