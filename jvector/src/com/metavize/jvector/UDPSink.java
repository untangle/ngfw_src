/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: UDPSink.java,v 1.10 2005/02/07 08:25:09 rbscott Exp $
 */

package com.metavize.jvector;

import com.metavize.jnetcap.*;

public class UDPSink extends Sink
{
    protected SinkEndpointListener listener = null;

    protected final IPTraffic traffic;

    /* Flag for the write function to indicate when ttl or tos is unused */
    protected static final int DISABLED = -1;

    public UDPSink( IPTraffic traffic )
    {
        /* Must lock the traffic structure so no one can modify where data is going */
        traffic.lock();

        this.traffic = traffic;

        /* XXX After the conversion remove the (int) */
        pointer        = create( (int)traffic.pointer());
    }

    public UDPSink( IPTraffic traffic, SinkEndpointListener listener )
    {
        this( traffic );
        registerListener( listener );
    }

    /**
     * Set the TTL for DataCrumbs.
     */
    public void ttl( byte ttl )
    {
        traffic.ttl( ttl );
    }

    public void registerListener( SinkEndpointListener listener )
    {
        this.listener = listener;
    }
    
    protected int send_event( Crumb o )
    {
        switch ( o.type() ) {
        case Crumb.TYPE_DATA:
        case Crumb.TYPE_UDP_PACKET:
            return write( (DataCrumb)o );
            
        case Crumb.TYPE_RESET:
            /* XXX Do whatever is necessary for a reset */
            Vector.logDebug( "Received a reset event" );

            /*  FALL THROUGH */
        case Crumb.TYPE_SHUTDOWN:
            return Vector.ACTION_SHUTDOWN;
            
        default:
            /* XXX What to do here */
            Vector.logError( "Unknown event type: " + o );
            return Vector.ACTION_ERROR;
        }
    }

    protected int write( DataCrumb crumb )
    {
        int numWritten;
        int ttl = DISABLED;
        int tos = DISABLED;
        byte[] options = null;

        int size = crumb.limit() - crumb.offset();

        if ( crumb.type() == Crumb.TYPE_UDP_PACKET ) {
            UDPPacketDesc desc = ((UDPPacketCrumb)(crumb)).desc();
            
            ttl = desc.ttl();
            tos = desc.tos();
            options = desc.options();
        }

        /* XXX Change to int once the conversion is complete */
        numWritten = write((int)traffic.pointer(), crumb.data(), crumb.offset(), size, ttl, tos, options );

        if ( numWritten < 0 ) {
            Vector.logError( "UDP: Unable to write crumb" );
            
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

    protected void sinkRaze()
    {
        super.sinkRaze();
        
        /* XXX ??? Maybe */
        // traffic.raze();
    }

    protected int shutdown()
    {
        /* Notify the listeners that sink is shutting down */
        if ( listener != null ) listener.shutdownEvent( this );

        return shutdown( pointer );
    }

    protected native int create( int pointer );

    /**
     * Send out a packet.</p>
     * @param pointer - Pointer to the traffic structure (netcap_pkt_t/IPTraffic.pointer)
     * @param data    - byte array of the data to send out
     * @param offset  - Offset within the byte array, this allows for multiple writes if one write
     *                  cannot accomodate the entire byte buffer.
     * @param size    - Total size of the byte array.
     * @param ttl     - TTL for the outgoing packet, or -1 if unused.
     * @param tos     - TOS for outgoing packet or -1 if unused.
     * @param options - options for the outgoing packet or -1 if unused (currently not implemented)
     * @return Number of bytes written
     */
    // protected static native int write( int pointer, byte[] data, int offset, int size, int packet );
    protected static native int write( int pointer, byte[] data, int offset, int size, int ttl,
                                       int tos, byte[] options );
    protected static native int shutdown( int pointer );
}
