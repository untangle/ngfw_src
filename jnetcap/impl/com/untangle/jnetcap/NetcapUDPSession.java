/**
 * $Id$
 */
package com.untangle.jnetcap;

@SuppressWarnings("unused") //JNI
public class NetcapUDPSession extends NetcapSession 
{
    private final static int FLAG_TTL            = 200;
    private final static int FLAG_TOS            = 201;
    
    private final UDPPacketMailbox clientMailbox;
    private final UDPPacketMailbox serverMailbox;

    private UDPAttributes serverTraffic = null;
    private UDPAttributes clientTraffic = null;
    
    public NetcapUDPSession( long id ) 
    {
        super( id );           
        
        clientMailbox = new UDPSessionMailbox( true );
        serverMailbox = new UDPSessionMailbox( false );
    }
    
    public UDPPacketMailbox clientMailbox()
    {
        return clientMailbox;
    }    

    public UDPPacketMailbox serverMailbox()
    {
        return serverMailbox;
    }
    
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
    
    public void serverComplete( UDPAttributes serverTraffic )
    {
        // nothing needed here
    }

    public void setServerTraffic(UDPAttributes serverTraffic)
    {
        this.serverTraffic = serverTraffic;
    }

    public void setClientTraffic(UDPAttributes clientTraffic)
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

        //the mark set on packets sent to the client are reversed
        //so if the connmark is 0x00000102 then its from interface 2->1
        //in this case the client mark is 0x00000201 because packets sent to the client
        //are 1->2. So we don't want to write the mark to the connmark because it would
        //be incorrect. As such, we set the connmark when we set the server mark
        //setSessionMark(pointer.value(), newmark);
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

        // set the connmark also
        setSessionMark(pointer.value(), newmark);
    }
    
    private static native long   read( long sessionPointer, boolean isClientSide, int timeout );
    private static native byte[] data( long packetPointer );
    private static native int    getData( long packetPointer, byte[] buffer );

    private static native long   mailboxPointer( long sessionPointer, boolean isClientSide );
    
    /* This is for sending the data associated with a netcap_pkt_t structure */
    private static native int  send( long packetPointer );

    /* Complete a session that was previously captured */
    private static native void serverComplete( long sessionPointer );

    /* Set the Session mark */
    private static native void setSessionMark( long sessionPointer, int mark );
    
    class UDPSessionMailbox implements UDPPacketMailbox
    {
        private final boolean isClientSide;

        UDPSessionMailbox( boolean isClientSide ) {
            this.isClientSide = isClientSide;
        }

        public UDPPacket read( int timeout )
        {
            CPointer packetPointer = new CPointer( NetcapUDPSession.read( pointer.value(), isClientSide, timeout ));
            
            UDPAttributes ipTraffic = new UDPAttributes( packetPointer );
            
            if (ipTraffic.getProtocol() != Netcap.IPPROTO_UDP) {
                int tmp = ipTraffic.getProtocol();
                /* Must free the packet */
                ipTraffic.raze();

                throw new IllegalStateException( "Packet is not UDP: " +  tmp );
            }
                
            return new PacketMailboxUDPPacket( packetPointer );
        }

        public UDPPacket read() 
        {
            return read( 0 );
        }

        public long pointer()
        {
            return NetcapUDPSession.mailboxPointer( pointer.value(), isClientSide );
        }

        abstract class PacketMailboxPacket implements UDPPacket
        {
            private final CPointer pointer;
            protected final UDPAttributes attributes;
            
            PacketMailboxPacket( CPointer pointer ) 
            {
                this.pointer = pointer;
                this.attributes = makeAttributes( pointer );
            }
            
            public UDPAttributes attributes()
            {
                return attributes;
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
                attributes.raze();
            }
            
            protected abstract UDPAttributes makeAttributes( CPointer pointer );
        }
        
        class PacketMailboxUDPPacket extends PacketMailboxPacket implements UDPPacket
        {
            PacketMailboxUDPPacket( CPointer pointer )
            {
                super( pointer );
            }

            protected UDPAttributes makeAttributes( CPointer pointer )
            {
                return new UDPAttributes( pointer );
            }
        }

    }
}
