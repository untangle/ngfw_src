/**
 * $Id$
 */
package com.untangle.jvector;

import com.untangle.jnetcap.*;
import java.util.EmptyStackException;

/**
 * UDPSource is a source for UDP packets
 */
public class UDPSource extends Source
{
    protected SourceEndpointListener listener = null;

    protected int byteCount  = 0;
    protected int chunkCount = 0;
    
    protected static final int READ_TIMEOUT = 10;

    protected final UDPPacketMailbox mailbox;

    /**
     * UDPSource
     * @param mailbox
     */
    public UDPSource( UDPPacketMailbox mailbox )
    {
        this.mailbox = mailbox;
        pointer = create( mailbox.pointer() );
    }

    /**
     * UDPSource
     * @param mailbox
     * @param listener
     */
    public UDPSource( UDPPacketMailbox mailbox, SourceEndpointListener listener )
    {
        this( mailbox );
        registerListener( listener );
    }   

    /**
     * registerListener - register a listener for this Source
     * @param listener
     */
    public void registerListener( SourceEndpointListener listener )
    {
        this.listener = listener;
    }
    
    /**
     * get_event - gets an event (packet/crumb) from this UDPSource
     * @param unused
     * @return the crumb
     */
    protected Crumb get_event( Sink unused )
    {
        PacketCrumb crumb;
        UDPPacket packet;

        try {
            packet = mailbox.read( READ_TIMEOUT );
        } catch ( EmptyStackException e ) {
            return ShutdownCrumb.getInstance();
        }
        
        if ( packet.attributes().pointer() == 0 ) {
            Vector.logError( "No packet to receive from the mailbox" );
            /* Return a shutdown crumb */
            return ShutdownCrumb.getInstance();
        }
        
        try {
            crumb = PacketCrumb.makeCrumb( packet );

            /* Notify listeners that data was received*/
            if ( listener != null ) listener.dataEvent( this, crumb.limit());
        } catch ( Exception e ) {
            Vector.logError( "Error getting crumb, shutting down " + e );
            return ShutdownCrumb.getInstance();
        } finally {
            packet.raze();
        }
        
        return crumb;
    }

    /**
     * shutdown - shutdown (close) this UDPSource
     * @return
     */
    protected int shutdown()
    {
        /* Notify the listeners that source is shutting down */
        if ( listener != null ) listener.shutdownEvent( this );

        return shutdown( pointer, mailbox.pointer());
    }

    /**
     * Create the C component of a UDPSource.
     *
     * @param pointer - Pointer to the UDP mailbox.
     * @return long (ptr)
     */
    protected native long create( long pointer );

    /**
     * shutdown - shutdown (close) this UDPSource
     * @param pointer
     * @param mailboxPointer
     * @return
     */
    protected static native int shutdown( long pointer, long mailboxPointer );
}
