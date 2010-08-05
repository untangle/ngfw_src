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

package com.untangle.jnetcap;

import java.net.InetAddress;

@SuppressWarnings("unused") //JNI
public class NetcapUDPSession extends NetcapSession 
{
    protected static final int MERGED_DEAD = 0xDEAD00D;

    private static final int DEFAULT_LIBERATE_FLAGS = 0;
    //unused private static final int DEFAULT_SERVER_COMPLETE_FLAGS = 0;
    
    /** These cannot conflict with the flags inside of NetcapTCPSession and NetcapSession */
    private final static int FLAG_TTL            = 64;
    private final static int FLAG_TOS            = 65;
    private final static int FLAG_ICMP_CLIENT_ID = 66;
    private final static int FLAG_ICMP_SERVER_ID = 67;
    private final static int FLAG_IS_ICMP        = 68;
    
    private final PacketMailbox clientMailbox;
    private final PacketMailbox serverMailbox;

    private IPTraffic serverTraffic = null;
    private IPTraffic clientTraffic = null;
    
    public NetcapUDPSession( int id ) 
    {
        super( id, Netcap.IPPROTO_UDP );           
        
        clientMailbox = new UDPSessionMailbox( true );
        serverMailbox = new UDPSessionMailbox( false );
    }
    
    public PacketMailbox clientMailbox() { return clientMailbox; }    
    public PacketMailbox serverMailbox() { return serverMailbox; }
    
    public byte ttl()
    { 
        return (byte) getIntValue( FLAG_TTL, pointer.value()); 
    }

    public byte tos()
    { 
        return (byte) getIntValue( FLAG_TOS, pointer.value());
    }

    /* XXX ICMP Hack */
    public boolean isIcmpSession()
    {
        return (( getIntValue( FLAG_IS_ICMP, pointer.value()) == 1 ) ? true : false );
    }

    /* XXX ICMP Hack */
    public int icmpClientId()
    { 
        return  getIntValue( FLAG_ICMP_CLIENT_ID, pointer.value());
    }
    
    /* XXX ICMP Hack */
    public int icmpServerId()
    {
        return  getIntValue( FLAG_ICMP_SERVER_ID, pointer.value());
    }
    
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
    public boolean merge( IPTraffic traffic, byte intf )
    {
        int ret  = merge( pointer.value(),
                          Inet4AddressConverter.toLong( traffic.dst().host()), traffic.dst().port(),
                          Inet4AddressConverter.toLong( traffic.src().host()), traffic.src().port(),
                          intf );
        
        if ( ret == MERGED_DEAD ) {
            return false;
        } else if ( ret == 0 ) {
            return true;
        } else {
            Netcap.error( "Invalid merge" );
        }
        
        return false;
    }

    /**
     * Merge this session with any other ICMP (treated as UDP for now) sessions started at the same time.</p>
     * @param traffic - Description of the traffic going to the server (dst should refer
     *                  to the server endpoint).
     * @return Returns whether or not the session was merged, or merged out.  True If this session
     *         should continue, false if this session was merged out.
     */
    public boolean icmpMerge( IPTraffic traffic, int id, byte intf )
    {
        int ret  = icmpMerge( pointer.value(), id,
                              Inet4AddressConverter.toLong( traffic.dst().host()),
                              Inet4AddressConverter.toLong( traffic.src().host()),
                              intf );
        
        if ( ret == MERGED_DEAD ) {
            return false;
        } else if ( ret == 0 ) {
            return true;
        } else {
            Netcap.error( "Invalid merge" );
        }
        
        return false;
    }

    /**
     * liberate the connection.
     */
    public void liberate()
    {
        liberate( pointer.value(), DEFAULT_LIBERATE_FLAGS );
    }

    /**
     * Complete a connection.
     */
    public void serverComplete( IPTraffic serverTraffic )
    {
        /* Move the first packet over to the server sink, this is used to confirm 
         * The conntrack entry */
        transferFirstPacketID( pointer.value(), serverTraffic.pointer());
        // serverComplete( pointer.value(), DEFAULT_SERVER_COMPLETE_FLAGS );
    }

    public void setServerTraffic(IPTraffic serverTraffic)
    {
        this.serverTraffic = serverTraffic;
    }

    public void setClientTraffic(IPTraffic clientTraffic)
    {
        this.clientTraffic = clientTraffic;
    }

    @Override
    public int  clientMark()
    {
        return this.clientTraffic.mark();
    }
    
    @Override
    public void clientMark(int newmark)
    {
        this.clientTraffic.mark(newmark);
    }

    @Override
    public int  serverMark()
    {
        return this.serverTraffic.mark();
    }
    
    @Override
    public void serverMark(int newmark)
    {
        this.serverTraffic.mark(newmark);
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
                                        long srcAddr, int srcPort, long dstAddr, int dstPort, byte intf );

    /**
     * Merge this session with any other ICMP/UDP session that may have started in the reverse
     * direction.</p>
     *
     * @param sessionPointer - Pointer to the udp session.
     * @param id - Session identifier in the ICMP message.
     * @param srcAddr - Source address(server side, server address)
     * @param dstAddr - Destination address(server side, client address)
     */
    private static native int    icmpMerge( long sessionPointer, int id, long srcAddr, long dstAddr, 
                                            byte intf );

    private static native long    mailboxPointer( long sessionPointer, boolean ifClient );
    
    /* This is for sending the data associated with a netcap_pkt_t structure */
    private static native int  send( long packetPointer );

    /* Release a session that was previously captured */
    private static native void liberate( long sessionPointer, int flags );

    /* Complete a session that was previously captured */
	private static native void serverComplete( long sessionPointer, int flags );

    /* Move over the first packet ID in the session */
    private static native void transferFirstPacketID( long sessionPointer, long serverTraffic );
    
    class UDPSessionMailbox implements PacketMailbox
    {
        private final boolean ifClient;

        UDPSessionMailbox( boolean ifClient ) {
            this.ifClient = ifClient;
        }

        public Packet read( int timeout )
        {
            CPointer packetPointer = new CPointer( NetcapUDPSession.read( pointer.value(), ifClient, timeout ));
            
            IPTraffic ipTraffic = new IPTraffic( packetPointer );
            
            /* XXX ICMP Hack */
            switch ( ipTraffic.protocol()) {
            case Netcap.IPPROTO_UDP:
                return new PacketMailboxUDPPacket( packetPointer );
            case Netcap.IPPROTO_ICMP:
                return new PacketMailboxICMPPacket( packetPointer );
            default:
                int tmp = ipTraffic.protocol();

                /* Must free the packet */
                ipTraffic.raze();
                throw new IllegalStateException( "Packet is neither ICMP or UDP: " +  tmp );
            }
        }

        public Packet read() 
        {
            return read( 0 );
        }

        public long pointer()
        {
            return NetcapUDPSession.mailboxPointer( pointer.value(), ifClient );
        }

        abstract class PacketMailboxPacket implements Packet
        {
            private final CPointer pointer;
            protected final IPTraffic traffic;
            
            PacketMailboxPacket( CPointer pointer ) 
            {
                this.pointer = pointer;
                this.traffic = makeTraffic( pointer );
            }
            
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
            
            protected abstract IPTraffic makeTraffic( CPointer pointer );
        }
        
        class PacketMailboxUDPPacket extends PacketMailboxPacket implements UDPPacket
        {
            PacketMailboxUDPPacket( CPointer pointer )
            {
                super( pointer );
            }

            protected IPTraffic makeTraffic( CPointer pointer )
            {
                return new IPTraffic( pointer );
            }
        }
        
        class PacketMailboxICMPPacket extends PacketMailboxPacket implements ICMPPacket
        {
            final byte icmpType;
            final byte icmpCode;
            
            PacketMailboxICMPPacket( CPointer pointer )
            {
                super( pointer );
                
                icmpType = ((ICMPTraffic)traffic).icmpType();
                icmpCode = ((ICMPTraffic)traffic).icmpCode();
            }
            
            public byte icmpType()
            {
                return icmpType;
            }
            
            public byte icmpCode()
            {
                return icmpCode;
            }

            public InetAddress icmpSource( byte data[], int limit )
            {
                return ((ICMPTraffic)traffic).icmpSource( data, limit );
            }
            
            protected IPTraffic makeTraffic( CPointer pointer )
            {
                return new ICMPTraffic( pointer );
            }

        }

    }
}
