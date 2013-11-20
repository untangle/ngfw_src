/**
 * $Id$
 */
package com.untangle.jnetcap;

public interface TCPEndpoints extends Endpoints {
    public int fd();

    /**
     * Configure a TCP File descriptor for blocking or non-blocking mode.<p/>
     *
     * @param mode <code>true</code> enable blocking, <code>false</code> to disable blocking.
     */
    public void blocking( boolean mode );

    public int read( byte[] data );

    public int write( byte[] data );
    public int write( String data );

    public void close();
}
