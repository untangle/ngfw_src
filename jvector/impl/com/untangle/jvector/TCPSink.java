/**
 * $Id$
 */
package com.untangle.jvector;

public class TCPSink extends Sink
{
    static final int WRITE_RETURN_IGNORE = -3;

    protected SinkEndpointListener listener = null;

    public TCPSink( int fd )
    {
        pointer = create( fd );
    }

    public TCPSink( int fd, SinkEndpointListener listener )
    {
        this( fd );
        registerListener( listener );
    }

    public void registerListener( SinkEndpointListener listener )
    {
        this.listener = listener;
    }
    
    @SuppressWarnings("fallthrough")
    protected int send_event( Crumb crumb )
    {
        if ( Vector.isDebugEnabled())
            Vector.logDebug( "send_event(" + this + "): crumb " + crumb );

        switch ( crumb.type() ) {
        case Crumb.TYPE_DATA:
            if ( crumb instanceof FakeDataCrumb ) {
                return splice( (FakeDataCrumb)crumb );
            } else {
                return write( (DataCrumb)crumb );
            }

        case Crumb.TYPE_RESET:
            Vector.logDebug( "Writing a reset crumb" );
            reset( pointer );

            // fallthrough
        case Crumb.TYPE_SHUTDOWN:
            return Vector.ACTION_SHUTDOWN;
            
        default:
            Vector.logError( "Unknown event type: " + crumb );
            return Vector.ACTION_ERROR;
        }
    }

    protected int splice( FakeDataCrumb crumb )
    {
        TCPSource src = (TCPSource)crumb.getSource();

        int numWritten = splice( this.pointer, src.pointer );

        if ( numWritten == 0 ) {
            /* XXX not sure what to do here */
            return Vector.ACTION_SHUTDOWN;
        }
        
        if ( src != null  && src.listener != null ) src.listener.dataEvent( src, numWritten );
        if ( listener != null ) listener.dataEvent( this, numWritten );

        return Vector.ACTION_DEQUEUE;
    }

    protected int write( DataCrumb crumb )
    {
        int numWritten;

        int offset = crumb.offset();
        int size = crumb.limit() - offset;
        
        numWritten = write( pointer, crumb.data(), offset, size );

        if ( Vector.isDebugEnabled()) {
            Vector.logDebug( "jvector: " + this + ": wrote " + numWritten + " bytes.");
        }
        
        if ( numWritten < 0 ) {
            if ( numWritten != WRITE_RETURN_IGNORE ) {
                Vector.logError( "TCP: Unable to write crumb" );
            }
            return Vector.ACTION_SHUTDOWN;
        }

        /* Notify listeners that data was transmitted */
        if ( listener != null ) listener.dataEvent( this, numWritten );
        
        if ( numWritten < size ) {
            crumb.advance( numWritten );
            return Vector.ACTION_NOTHING;
        }

        return Vector.ACTION_DEQUEUE;
    }

    public void raze()
    {
        if ( pointer != 0L ) {
            close( pointer );
            super.raze();
        }

        pointer = 0L;
    }

    protected void sinkRaze()
    {
        if ( pointer != 0L ) {
            close( pointer );
            super.sinkRaze();
        }
        pointer = 0L;
    }

    protected int shutdown()
    {
        /* Notify the listeners that sink is shutting down */
        if ( listener != null ) listener.shutdownEvent( this );

        return shutdown( pointer );
    }

    protected native long create( int fd );
    protected static native int write( long snk_ptr, byte[] data, int offset, int size );
    protected static native int splice( long snk_ptr, long src_ptr );
    protected static native int close( long snk_ptr );
    protected static native void reset( long snk_ptr );
    protected static native int shutdown( long snk_ptr );
}
