/**
 * $Id$
 */
package com.untangle.jnetcap;

import java.net.InetAddress;

@SuppressWarnings("unused") //JNI
public class NetcapUDPSession extends NetcapSession 
{
    protected static final int MERGED_DEAD = 0xDEAD00D;

    /** These cannot conflict with the flags inside of NetcapTCPSession and NetcapSession */
    private final static int FLAG_TTL            = 64;
    private final static int FLAG_TOS            = 65;
    
    private final PacketMailbox clientMailbox;
    private final PacketMailbox serverMailbox;

    private IPTraffic serverTraffic = null;
    private IPTraffic clientTraffic = null;
    
    public NetcapUDPSession( long id ) 
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
    public boolean merge( IPTraffic traffic, int intf )
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
     * Complete a connection.
     */
    public void serverComplete( IPTraffic serverTraffic )
    {
        // serverComplete( pointer.value() );
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

        setSessionMark(pointer.value(), newmark & 0xffffff00);
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

        setSessionMark(pointer.value(), newmark & 0xffffff00);
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
    private static native int    merge( long sessionPointer, long srcAddr, int srcPort, long dstAddr, int dstPort, int intf );

    private static native long    mailboxPointer( long sessionPointer, boolean ifClient );
    
    /* This is for sending the data associated with a netcap_pkt_t structure */
    private static native int  send( long packetPointer );

    /* Complete a session that was previously captured */
    private static native void serverComplete( long sessionPointer );

    /* Set the Session mark */
    private static native void setSessionMark( long sessionPointer, int mark );
    
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
            
            if (ipTraffic.getProtocol() != Netcap.IPPROTO_UDP) {
                int tmp = ipTraffic.getProtocol();
                /* Must free the packet */
                ipTraffic.raze();

                throw new IllegalStateException( "Packet is not UDP: " +  tmp );
            }
                
            return new PacketMailboxUDPPacket( packetPointer );
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

    }
}
