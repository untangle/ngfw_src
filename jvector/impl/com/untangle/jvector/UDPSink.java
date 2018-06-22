/**
 * $Id$
 */
package com.untangle.jvector;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.*;

/**
 * UDPSink is a sink for UDP packets
 */
public class UDPSink extends Sink
{
    private final Logger logger = Logger.getLogger(getClass());

    protected SinkEndpointListener listener = null;

    protected final UDPAttributes traffic;

    /* Flag for the write function to indicate when ttl or tos is unused */
    protected static final int DISABLED = -1;

    /**
     * UDPSink
     * @param traffic
     * @param listener
     */
    public UDPSink( UDPAttributes traffic, SinkEndpointListener listener)
    {
        /* Must lock the traffic structure so no one can modify where data is going */
        traffic.lock();

        this.traffic = traffic;

        // 4/08 -- We're after conversion now, so it's been removed.
        // WAS:
        /*      xxx After the conversion remove the (int), (may need longs for 64-bit) */
        pointer = create( traffic.pointer());
        
        registerListener( listener );        
    }

    /**
     * Set the TTL for DataCrumbs.
     * @param ttl
     */
    public void ttl( byte ttl )
    {
        traffic.ttl( ttl );
    }

    /**
     * registerListener
     * @param listener
     */
    public void registerListener( SinkEndpointListener listener )
    {
        this.listener = listener;
    }
    
    /**
     * send_event
     * @param o
     * @return
     */
    protected int send_event( Crumb o )
    {
        switch ( o.type() ) {
        case Crumb.TYPE_DATA:
        case Crumb.TYPE_UDP_PACKET:
            return write((DataCrumb)o );
            
        case Crumb.TYPE_RESET:
            /* XXX Do whatever is necessary for a reset */
            Vector.logDebug( "Received a reset event" );
            return Vector.ACTION_SHUTDOWN;

        case Crumb.TYPE_SHUTDOWN:
            return Vector.ACTION_SHUTDOWN;
            
        default:
            /* XXX What to do here */
            Vector.logError( "Unknown event type: " + o );
            return Vector.ACTION_ERROR;
        }
    }

    /**
     * write
     * @param crumb
     * @return
     */
    protected int write( DataCrumb crumb )
    {
        int numWritten;
        int ttl = DISABLED;
        int tos = DISABLED;
        byte[] options = null;
        long sourceAddress = 0;

        int size = crumb.limit() - crumb.offset();

        if (crumb.type() != Crumb.TYPE_UDP_PACKET) {
            logger.warn("Unknown Crumb type: " + crumb.type());
        }
        else {
            PacketCrumb packetCrumb = (PacketCrumb)crumb;
            ttl = packetCrumb.ttl();
            tos = packetCrumb.tos();
        }

        numWritten = write(traffic.pointer(), crumb.data(), crumb.offset(), size, ttl, tos, options, sourceAddress );

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
    
    /**
     * sinkRaze
     */
    protected void sinkRaze()
    {
        super.sinkRaze();
        
        /* Since the traffic structure is passed in, it is the callers responsibility to
         * raze the traffic structure, this is presently done in UDPHook.java */
        // traffic.raze();
    }

    /**
     * shutdown
     * @return
     */
    protected int shutdown()
    {
        /* Notify the listeners that sink is shutting down */
        if ( listener != null ) listener.shutdownEvent( this );

        return shutdown( pointer );
    }

    /**
     * create
     * @param pointer
     * @return long (ptr)
     */
    protected native long create( long pointer );

    /**
     * Send out a ICMP or UDP packet.
     * @param pointer - Pointer to the traffic structure (netcap_pkt_t/UDPAttributes.pointer)
     * @param data    - byte array of the data to send out
     * @param offset  - Offset within the byte array, this allows for multiple writes if one write
     *                  cannot accomodate the entire byte buffer.
     * @param size    - Total size of the byte array.
     * @param ttl     - TTL for the outgoing packet, or -1 if unused.
     * @param tos     - TOS for outgoing packet or -1 if unused.
     * @param options - options for the outgoing packet or null if unused (currently not implemented)
     * @param src     - Source address, used only for an ICMP message. (unused if zero)
     * @return Number of bytes written
     */
    protected static native int write( long pointer, byte[] data, int offset, int size, int ttl, int tos, byte[] options, long src );

    /**
     * shutdown
     * @param pointer
     * @return
     */
    protected static native int shutdown( long pointer );
}
