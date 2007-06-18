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

import com.untangle.jnetcap.*;
import java.util.EmptyStackException;

public class UDPSource extends Source
{
    protected SourceEndpointListener listener = null;

    protected int byteCount  = 0;
    protected int chunkCount = 0;
    
    protected static final int READ_TIMEOUT = 10;

    protected final PacketMailbox mailbox;

    public UDPSource( PacketMailbox mailbox )
    {
        this.mailbox = mailbox;
        pointer = create( mailbox.pointer() );
    }

    public UDPSource( PacketMailbox mailbox, SourceEndpointListener listener )
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
        PacketCrumb crumb;

        /* XXX How should we handle the byte array */
        Packet packet;

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
