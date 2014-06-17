/**
 * $Id$
 */
package com.untangle.jvector;

public class TCPSource extends Source
{
    protected SourceEndpointListener listener = null;
    
    private static final int MAX_READ_SIZE = 8 * 1024;
    private static final int READ_RESET = -1;

    public TCPSource( int fd )
    {
        pointer = create( fd );
    }

    public TCPSource( int fd, SourceEndpointListener listener )
    {
        this( fd );
        registerListener( listener );
    }

    public void registerListener( SourceEndpointListener listener )
    {
        this.listener = listener;
    }
    
    protected Crumb get_event( Sink sink )
    {
        int ret;
        Crumb crumb;

        int bytes_available = peek( pointer );
        
        /**
         * Splice implementation - disabled
         * disable the use of splice optimization for now (bug #11885)
         */

        // if ( sink instanceof TCPSink ) {
        //     /**
        //      * We only send a FakeDataCrumb if data is available
        //      * If not, do a traditional read so resets and closes
        //      * are handled like normal
        //      */
        //     if ( bytes_available > 0 ) {
        //         crumb = new FakeDataCrumb( this );
        //         return crumb;
        //     }
        // }

        int readSize;
        if ( bytes_available > 0 && bytes_available < MAX_READ_SIZE )
            readSize = bytes_available + 1;
        else
            readSize = MAX_READ_SIZE;
        
        byte[] data = new byte[ readSize ];
        ret = read( pointer, data );
        
        switch( ret ) {
        case READ_RESET:
            crumb = ResetCrumb.getInstance();
            break;

        case 0:
            crumb = ShutdownCrumb.getInstance();
            break;
            
        default:
            /* Notify listeners that data was received */
            if ( listener != null ) listener.dataEvent( this, ret );

            crumb = new DataCrumb( data, ret );
        }
        
        if ( Vector.isDebugEnabled())
            Vector.logDebug( "get_event(" + this + "): crumb " + crumb );

        return crumb;
    }

    protected int shutdown()
    {
        /* Notify the listeners that source is shutting down */
        if ( listener != null ) listener.shutdownEvent( this );

        return shutdown( pointer );
    }

    protected native long create( int fd );

    /**
     * Read data from the associated filedescriptor.</p>
     * @param pointer - Pointer to the TCP Sink.
     * @param data    - Byte array where the data should be placed.
     * @return The number of characters placed into <code>data</code>, 0 if the fd is
     *         shutdown, or READ_RESET if there was a reset.
     * Throws an error if the file descriptor is invalid, or there is an error
     * reading data from the file descriptor
     */
    protected static native int read( long pointer, byte[] data );
    protected static native int peek( long pointer );
    protected static native int shutdown( long pointer );
}
