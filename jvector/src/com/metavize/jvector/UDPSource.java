/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: UDPSource.java,v 1.11 2005/01/31 03:17:31 rbscott Exp $
 */

package com.metavize.jvector;

import com.metavize.jnetcap.*;
import java.util.EmptyStackException;

public class UDPSource extends Source
{
    protected SourceEndpointListener listener = null;

    protected int byteCount  = 0;
    protected int chunkCount = 0;
    
    protected static final int READ_TIMEOUT = 10;

    protected final UDPMailbox mailbox;

    public UDPSource( UDPMailbox mailbox )
    {
        this.mailbox = mailbox;
        pointer = create( mailbox.pointer() );
    }

    public UDPSource( UDPMailbox mailbox, SourceEndpointListener listener )
    {
        this( mailbox );
        registerListener( listener );
    }   

    public void registerListener( SourceEndpointListener listener )
    {
        this.listener = listener;
    }
    
    protected Crumb get_event()
    {
        /* XXX How should we handle the byte array */
        UDPPacket packet;

        /* XXX How to handle ICMP, and other such fun stuff */
        try {
            packet = mailbox.read( READ_TIMEOUT );
        } catch ( EmptyStackException e ) {
            return ShutdownCrumb.getInstance();
        }
        
        if ( packet.traffic().pointer() == 0 ) {
            Vector.logError( "No packet to receive from the mailbox" );
            /* Return a shutdown crumb */
            return ShutdownCrumb.getInstance();
        }
        
        UDPPacketCrumb crumb = new UDPPacketCrumb( new UDPPacketDesc( packet ), packet.data());

        /* Notify listeners that data was received */
        if ( listener != null ) listener.dataEvent( this, crumb.limit());
        
        packet.raze();
        
        return crumb;
    }

    protected int shutdown()
    {
        /* Notify the listeners that source is shutting down */
        if ( listener != null ) listener.shutdownEvent( this );

        return shutdown( pointer, mailbox.pointer());
    }

    /**
     * Create the C component of a UDPSource.</p>
     *
     * @param pointer - Pointer to the UDP mailbox.
     */
    protected native int create( int pointer );
    protected static native int shutdown( int pointer, int mailboxPointer );
}
