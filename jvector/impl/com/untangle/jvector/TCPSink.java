/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
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
    
    protected int send_event( Crumb crumb )
    {
        if ( Vector.isDebugEnabled())
            Vector.logDebug( "send_event(" + this + "): crumb " + crumb );

        switch ( crumb.type() ) {
        case Crumb.TYPE_DATA:
            return write( (DataCrumb)crumb );

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

    protected int write( DataCrumb crumb )
    {
        int numWritten;

        int offset = crumb.offset();
        int size = crumb.limit() - offset;
        
        numWritten = write( pointer, crumb.data(), offset, size );

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
        if ( pointer != 0 ) {
            close( pointer );
            super.raze();
        }

        pointer = 0;
    }

    protected void sinkRaze()
    {
        if ( pointer != 0 ) {
            close( pointer );
            super.sinkRaze();
        }
        pointer = 0;
    }

    protected int shutdown()
    {
        /* Notify the listeners that sink is shutting down */
        if ( listener != null ) listener.shutdownEvent( this );

        return shutdown( pointer );
    }

    protected native int create( int fd );
    protected static native int write( int pointer, byte[] data, int offset, int size );
    protected static native int close( int pointer );
    protected static native void reset( int pointer );
    protected static native int shutdown( int pointer );
}
