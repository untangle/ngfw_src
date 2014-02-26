/**
 * $Id$
 */
package com.untangle.jvector;

public class TCPSource extends Source
{
    protected SourceEndpointListener listener = null;

    private static int maxRead = 8 * 1024;

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
    
    protected Crumb get_event()
    {
        /* XXX How should we handle the byte array */
        byte[] data = new byte[maxRead];
        int ret;
        
        Crumb crumb;

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
    protected static native int shutdown( long pointer );
}
