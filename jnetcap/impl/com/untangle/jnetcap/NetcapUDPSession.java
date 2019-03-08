/**
 * $Id$
 */
package com.untangle.jnetcap;

/**
 *NetcapUDPSession
 */
@SuppressWarnings("unused") //JNI
public class NetcapUDPSession extends NetcapSession 
{
    private final static int FLAG_TTL            = 200;
    private final static int FLAG_TOS            = 201;
    
    private final UDPPacketMailbox clientMailbox;
    private final UDPPacketMailbox serverMailbox;

    private UDPAttributes serverTraffic = null;
    private UDPAttributes clientTraffic = null;
    
    /**
     * NetcapUDPSession
     * @param id - session id
     */
    public NetcapUDPSession( long id ) 
    {
        super( id );           
        
        clientMailbox = new UDPSessionMailbox( true );
        serverMailbox = new UDPSessionMailbox( false );
    }
    
    /**
     * clientMailbox gets the client-side mailbox 
     * @return the client mailbox
     */
    public UDPPacketMailbox clientMailbox()
    {
        return clientMailbox;
    }    

    /**
     * serverMailbox gets the server-side mailbox
     * @return the server mailbox
     */
    public UDPPacketMailbox serverMailbox()
    {
        return serverMailbox;
    }
    
    /**
     * the session TTL (used to when sending packets)
     * @return the session ttl
     */
    public byte ttl()
    { 
        return (byte) getIntValue( FLAG_TTL, pointer.value()); 
    }

    /**
     * the session TOS (used when sending packets)
     * @return the session tos
     */
    public byte tos()
    { 
        return (byte) getIntValue( FLAG_TOS, pointer.value());
    }

    /**
     * makeEndpoints - get an Endpoints object representing the tuple
     * @param ifClient true if client side, false if server side
     * @return endpoints
     */
    protected Endpoints makeEndpoints( boolean ifClient ) 
    {
        return new SessionEndpoints( ifClient );
    }
    
    /**
     * serverComplete - a no-op for UDP
     * @param serverTraffic 
     */
    public void serverComplete(UDPAttributes serverTraffic)
    {
        // nothing needed here
    }

    /**
     * setServerTraffic - set server side attributes
     * @param serverTraffic - the server side attributes
     */
    public void setServerTraffic(UDPAttributes serverTraffic)
    {
        this.serverTraffic = serverTraffic;
    }

    /**
     * setClientTraffic - set client side attributes
     * @param clientTraffic - the client side attributes
     */
    public void setClientTraffic(UDPAttributes clientTraffic)
    {
        this.clientTraffic = clientTraffic;
    }

    /**
     * clientMark gets the client side mark
     * @return the mark
     */
    @Override
    public int  clientMark()
    {
        return this.clientTraffic.mark();
    }
    
    /**
     * clientMark sets the client side mark
     * @param newmark - the new mark to use when sending packets to client
     */
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

    /**
     * serverMark gets the server side mark
     * @return the mark
     */
    @Override
    public int  serverMark()
    {
        return this.serverTraffic.mark();
    }
    
    /**
     * serverMark sets the server side mark
     * @param newmark - the new mark used to send packets to the server
     */
    @Override
    public void serverMark(int newmark)
    {
        this.serverTraffic.mark(newmark);

        // set the connmark also
        setSessionMark(pointer.value(), newmark);
    }
    
    /**
     * read
     * @param sessionPointer
     * @param isClientSide
     * @param timeout
     * @return how much is read
     */
    private static native long   read( long sessionPointer, boolean isClientSide, int timeout );

    /**
     * data
     * @param packetPointer
     * @return byte[]
     */
    private static native byte[] data( long packetPointer );

    /**
     * getData
     * @param packetPointer
     * @param buffer
     * @return how much is read
     */
    private static native int    getData( long packetPointer, byte[] buffer );

    /**
     * mailboxPointer - gets a pointer to the mailbox
     * @param sessionPointer
     * @param isClientSide
     * @return pointer
     */
    private static native long   mailboxPointer( long sessionPointer, boolean isClientSide );
    
    /**
     * This is for sending the data associated with a netcap_pkt_t structure
     * @param packetPointer
     * @return how much is sent
     */
    private static native int  send( long packetPointer );

    /**
     * Complete a session that was previously captured
     * @param sessionPointer
     */
    private static native void serverComplete( long sessionPointer );

    /**
     * Set the Session mark
     * @param sessionPointer
     * @param mark
     */
    private static native void setSessionMark( long sessionPointer, int mark );
    
    /**
     * UDPSessionMailbox
     */
    class UDPSessionMailbox implements UDPPacketMailbox
    {
        private final boolean isClientSide;

        /**
         * UDPSessionMailbox
         * @param isClientSide
         */
        UDPSessionMailbox( boolean isClientSide ) {
            this.isClientSide = isClientSide;
        }

        /**
         * read - reads the packet with specified timeout
         * @param timeout 
         * @return UDPPacket
         */
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

        /**
         * read the packetd
         * @return UDPPacket
         */
        public UDPPacket read() 
        {
            return read( 0 );
        }

        /**
         * pointer - get the C pointer
         * @return pointer
         */
        public long pointer()
        {
            return NetcapUDPSession.mailboxPointer( pointer.value(), isClientSide );
        }

        /**
         * PacketMailboxPacket
         */
        abstract class PacketMailboxPacket implements UDPPacket
        {
            private final CPointer pointer;
            protected final UDPAttributes attributes;
            
            /**
             * PacketMailboxPacket constructor
             * @param pointer - C pointer
             */
            PacketMailboxPacket( CPointer pointer ) 
            {
                this.pointer = pointer;
                this.attributes = makeAttributes( pointer );
            }
            
            /**
             * attributes gets the UDPAttributes
             * @return UDPAttributes
             */
            public UDPAttributes attributes()
            {
                return attributes;
            }
            
            /**
             * data returns the data
             * @return data
             */
            public byte[] data() 
            {
                return NetcapUDPSession.data( pointer.value());
            }

            /**
             * getData gets the data into the buffer
             * @param buffer 
             * @return amount of data
             */
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

            /**
             * raze (free) the attributes
             */
            public void raze() 
            {
                attributes.raze();
            }
            
            /**
             * makeAttributes
             * @param pointer
             * @return UDPAttributes
             */
            protected abstract UDPAttributes makeAttributes( CPointer pointer );
        }
        
        /**
         * PacketMailboxUDPPacket 
         */
        class PacketMailboxUDPPacket extends PacketMailboxPacket implements UDPPacket
        {
            /**
             * PacketMailboxUDPPacket constructor
             * @param pointer - the c pointer
             */
            PacketMailboxUDPPacket( CPointer pointer )
            {
                super( pointer );
            }

            /**
             * makeAttributes gets the attributes (from C)
             * @param pointer 
             * @return UDPAttributes
             */
            protected UDPAttributes makeAttributes( CPointer pointer )
            {
                return new UDPAttributes( pointer );
            }
        }

    }
}
