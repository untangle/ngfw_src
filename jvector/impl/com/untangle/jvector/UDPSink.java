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

import java.net.InetAddress;

import com.untangle.jnetcap.*;

/* This should really be called a packet sink since it writes both ICMP
 * and UDP packets */
public class UDPSink extends Sink
{
    protected SinkEndpointListener listener = null;

    private final ICMPMailbox icmpMailbox;
    
    protected final IPTraffic traffic;

    private final int icmpId;

    /* Flag for the write function to indicate when ttl or tos is unused */
    protected static final int DISABLED = -1;

    public UDPSink( IPTraffic traffic, SinkEndpointListener listener, ICMPMailbox icmpMailbox, int icmpId )
    {
        /* Must lock the traffic structure so no one can modify where data is going */
        traffic.lock();

        this.traffic = traffic;

        // 4/08 -- We're after conversion now, so it's been removed.
        // WAS:
        /*      xxx After the conversion remove the (int), (may need longs for 64-bit) */
        pointer = create( traffic.pointer());
        
        this.icmpMailbox = icmpMailbox;
        this.icmpId      = icmpId;
        
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
    
    @SuppressWarnings("fallthrough")
    protected int send_event( Crumb o )
    {
        switch ( o.type() ) {
        case Crumb.TYPE_DATA:
        case Crumb.TYPE_UDP_PACKET:
        case Crumb.TYPE_ICMP_PACKET:
            return write((DataCrumb)o );
            
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

    @SuppressWarnings("fallthrough")
    protected int write( DataCrumb crumb )
    {
        int numWritten;
        int ttl = DISABLED;
        int tos = DISABLED;
        byte[] options = null;
        boolean isUdp = true;
        long sourceAddress = 0;

        int size = crumb.limit() - crumb.offset();

        switch ( crumb.type()) {
        case Crumb.TYPE_ICMP_PACKET:
            ICMPPacketCrumb icmpCrumb = (ICMPPacketCrumb)crumb;
            InetAddress src;

            if ( crumb.offset() == 0 ) {
                /* This fixes the address and ports for an error packet */
                try {
                    /* Update to the new length */
                    int limit = icmpCrumb.updatePacket( icmpId, icmpMailbox );
                    /* This is for cases where the ICMP packet itself is not valid.  If
                     * there is an error, an exception is thrown. */
                    if ( limit <= 0 ) {
                        Vector.logWarn( "Dropping invalid ICMP Crumb. " );
                        return Vector.ACTION_DEQUEUE;
                    }

                    icmpCrumb.limit( limit );
                } catch( Exception e ) {
                    Vector.logWarn( "Unable to fix ICMP Crumb: " + e );
                    return Vector.ACTION_DEQUEUE;
                }
            }
            
            src = icmpCrumb.source();
            if ( src != null ) {
                sourceAddress = Inet4AddressConverter.toLong( src );
            }
            isUdp = false;
            /* fallthrough */
        case Crumb.TYPE_UDP_PACKET:
            PacketCrumb packetCrumb = (PacketCrumb)crumb;
            ttl = packetCrumb.ttl();
            tos = packetCrumb.tos();

            /* XXX need to implement options */
            // options = packetCrumb.options();

            /* Assume that it is a valid type */
        default:
        }

        numWritten = write(traffic.pointer(), crumb.data(), crumb.offset(), size, ttl, tos, 
                           options, isUdp, sourceAddress );

        if ( numWritten < 0 ) {
            Vector.logWarn( "UDP: Unable to write crumb" );
            
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
        
        /* Since the traffic structure is passed in, it is the callers responsibility to
         * raze the traffic structure, this is presently done in UDPHook.java */
        // traffic.raze();
    }

    protected int shutdown()
    {
        /* Notify the listeners that sink is shutting down */
        if ( listener != null ) listener.shutdownEvent( this );

        return shutdown( pointer );
    }

    protected native long create( long pointer );

    /**
     * Send out a ICMP or UDP packet.</p>
     * @param pointer - Pointer to the traffic structure (netcap_pkt_t/IPTraffic.pointer)
     * @param data    - byte array of the data to send out
     * @param offset  - Offset within the byte array, this allows for multiple writes if one write
     *                  cannot accomodate the entire byte buffer.
     * @param size    - Total size of the byte array.
     * @param ttl     - TTL for the outgoing packet, or -1 if unused.
     * @param tos     - TOS for outgoing packet or -1 if unused.
     * @param options - options for the outgoing packet or null if unused (currently not implemented)
     * @param isUdp   - True if this is a UDP packet, false if it is ICMP.
     * @param src     - Source address, used only for an ICMP message. (unused if zero)
     * @return Number of bytes written
     */
    // protected static native int write( int pointer, byte[] data, int offset, int size, int packet );
    protected static native int write( long pointer, byte[] data, int offset, int size, int ttl,
                                       int tos, byte[] options, boolean isUdp, long srcAddress );
    protected static native int shutdown( long pointer );
}
