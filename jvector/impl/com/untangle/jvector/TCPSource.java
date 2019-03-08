/**
 * $Id$
 */
package com.untangle.jvector;

/**
 * TCPSource is a source for TCP data (backed by a socket)
 */
public class TCPSource extends Source
{
    protected SourceEndpointListener listener = null;
    
    private static final int MAX_READ_SIZE = 8 * 1024;
    private static final int READ_RESET = -1;

    private boolean spliceEnabled = false;

    /**
     * TCPSource
     * @param fd
     */
    public TCPSource( int fd )
    {
        pointer = create( fd );
        spliceEnabled = (System.getProperty("uvm.tcp.splice") != null);
    }

    /**
     * TCPSource
     * @param fd
     * @param listener
     */
    public TCPSource( int fd, SourceEndpointListener listener )
    {
        this( fd );
        registerListener( listener );
    }

    /**
     * registerListener
     * @param listener
     */
    public void registerListener( SourceEndpointListener listener )
    {
        this.listener = listener;
    }
    
    /**
     * get_event
     * @param sink
     * @return
     */
    protected Crumb get_event( Sink sink )
    {
        int ret;
        Crumb crumb;

        int bytes_available = peek( pointer );
        
        /**
         * Splice optimization
         */
        if ( spliceEnabled && sink instanceof TCPSink ) {
            /**
             * We only send a FakeDataCrumb if data is available
             * If not, do a traditional read so resets and closes
             * are handled like normal
             */
            if ( bytes_available > 0 ) {
                crumb = new FakeDataCrumb( this );
                return crumb;
            }
        }

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
            if ( Vector.isDebugEnabled()) {
                Vector.logDebug( "jvector: " + this + ": read reset.");
            }
            break;
            
        case 0:
            crumb = ShutdownCrumb.getInstance();
            if ( Vector.isDebugEnabled()) {
                Vector.logDebug( "jvector: " + this + ": read shutdown.");
            }
            break;

        default:
            /* Notify listeners that data was received */
            if ( listener != null ) listener.dataEvent( this, ret );
            crumb = new DataCrumb( data, ret );
            if ( Vector.isDebugEnabled()) {
                Vector.logDebug( "jvector: " + this + ": read " + ret + " bytes.");
            }
            break;

        }

        return crumb;
    }

    /**
     * shutdown
     * @return
     */
    protected int shutdown()
    {
        /* Notify the listeners that source is shutting down */
        if ( listener != null ) listener.shutdownEvent( this );

        return shutdown( pointer );
    }

    /**
     * create
     * @param fd
     * @return
     */
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

    /**
     * peek
     * @param pointer
     * @return
     */
    protected static native int peek( long pointer );

    /**
     * shutdown
     * @param pointer
     * @return
     */
    protected static native int shutdown( long pointer );
}
