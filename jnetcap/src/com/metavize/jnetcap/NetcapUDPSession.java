/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: NetcapUDPSession.java,v 1.8 2005/01/06 20:55:52 rbscott Exp $
 */

package com.metavize.jnetcap;

import java.util.EmptyStackException;

public class NetcapUDPSession extends NetcapSession 
{
    protected static final int MERGED_DEAD = 0xDEAD00D;
    
    /** These cannot conflict with the flags inside of NetcapTCPSession and NetcapSession */
    private final static int FLAG_TTL         = 64;
    private final static int FLAG_TOS         = 65;


    private final UDPMailbox clientMailbox;
    private final UDPMailbox serverMailbox;

    public NetcapUDPSession( int id ) 
    {
        super( id, Netcap.IPPROTO_UDP );           
        
        clientMailbox = new UDPSessionMailbox( true );
        serverMailbox = new UDPSessionMailbox( false );
    }

    public UDPMailbox clientMailbox() { return clientMailbox; }    
    public UDPMailbox serverMailbox() { return serverMailbox; }

    public byte ttl() { return (byte) getIntValue( FLAG_TTL, pointer.value()); }
    public byte tos() { return (byte) getIntValue( FLAG_TOS, pointer.value()); }

    protected Endpoints makeEndpoints( boolean ifClient ) 
    {
        return new SessionEndpoints( ifClient );
    }

    /**
     * Merge this session with any other UDP sessions started at the same time.</p>
     * @param traffic - Description of the traffic going to the server (dst should refer
     *                  to the server endpoint).
     * @return Returns whether or not the session was merged, or merged out.  True If this session
     *         should continue, false if this session was merged out.
     */
    public boolean merge( IPTraffic traffic )
    {
        int ret  = merge( pointer.value(), 
                          Inet4AddressConverter.toLong( traffic.dst().host()), traffic.dst().port(),
                          Inet4AddressConverter.toLong( traffic.src().host()), traffic.src().port());
        
        if ( ret == MERGED_DEAD ) {
            return false;
        } else if ( ret == 0 ) {
            return true;
        } else {
            Netcap.error( "Invalid merge" );
        }
        
        return false;
    }
    
    private static native long   read( long sessionPointer, boolean ifClient, int timeout );
    private static native byte[] data( long packetPointer );
    private static native int    getData( long packetPointer, byte[] buffer );

    /**
     * Merge this session with any other UDP session that may have started in the reverse
     * direction.</p>
     *
     * @param sessionPointer - Pointer to the udp session.
     * @param srcAddr - Source address(server side, server address)
     * @param srcPort - Source port(server side, server port)
     * @param dstAddr - Destination address(server side, client address)
     * @param dstPort - Destination port(server side, client port)
     */
    private static native int    merge( long sessionPointer, 
                                        long srcAddr, int srcPort, long dstAddr, int dstPort );

    private static native int    mailboxPointer( long sessionPointer, boolean ifClient );
    
    /* This is for sending the data associated with a netcap_pkt_t structure */
    private static native int  send( long packetPointer );
    
    class UDPSessionMailbox implements UDPMailbox
    {
        private final boolean ifClient;

        UDPSessionMailbox( boolean ifClient ) {
            this.ifClient = ifClient;
        }

        public UDPPacket read( int timeout )
        {
            return new UDPMailboxPacket( NetcapUDPSession.read( pointer.value(), ifClient, timeout ));
        }

        public UDPPacket read() 
        {
            return new UDPMailboxPacket( NetcapUDPSession.read( pointer.value(), ifClient, 0 ));
        }

        public int pointer()
        {
            return NetcapUDPSession.mailboxPointer( pointer.value(), ifClient );
        }
        
        class UDPMailboxPacket implements UDPPacket {
            private final CPointer pointer;
            private final UDPSessionTraffic traffic;
                        
            public IPTraffic traffic() 
            {
                return traffic;
            }
            
            public byte[] data() 
            {
                return NetcapUDPSession.data( pointer.value());
            }

            public int getData( byte[] buffer )
            {
                return NetcapUDPSession.getData( pointer.value(), buffer );
            }

            /**
             * Send out this packet 
             */
            public void send() 
            {
                NetcapUDPSession.send( pointer.value());
            }

            public void raze() 
            {
                traffic.raze();
            }
            
            UDPMailboxPacket( long pointer ) 
            {
                this.pointer = new CPointer( pointer );
                
                traffic = new UDPSessionTraffic( this.pointer );
            }

            class UDPSessionTraffic extends IPTraffic 
            {
                /* This is so that the traffic structure will automatically NULL its pointer 
                 * if the parent is NULLed, also by implementing this as a subclass, this
                 * disallows the possibility of creating arbitrary IPTraffic structures with
                 * pointers from anywhere in java */
                UDPSessionTraffic( CPointer pointer ) 
                {
                    super( pointer );
                }
            }
        }
    }
}
