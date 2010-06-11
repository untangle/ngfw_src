/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
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
    protected static native int write( long pointer, byte[] data, int offset, int size );
    protected static native int close( long pointer );
    protected static native void reset( long pointer );
    protected static native int shutdown( long pointer );
}
