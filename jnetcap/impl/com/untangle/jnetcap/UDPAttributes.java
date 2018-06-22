/**
 * $Id$
 */
package com.untangle.jnetcap;

import java.net.InetAddress;

import org.apache.log4j.Logger;

/** Represents the attributes of the side (client side or server side) of a UDP session */
public class UDPAttributes
{
    protected static final Logger logger = Logger.getLogger( UDPAttributes.class );

    /**
     * The items with this bit are lockable, meaning once the lock flag is set
     * you are no longer able to modify these values
     */
    private static final int LOCKABLE_MASK  = 0x4000;
    private static final int FLAG_HOST      = 0x001 | LOCKABLE_MASK;
    private static final int FLAG_PORT      = 0x002 | LOCKABLE_MASK;
    private static final int FLAG_INTERFACE = 0x003 | LOCKABLE_MASK;
    private static final int FLAG_TTL       = 0x004;
    private static final int FLAG_TOS       = 0x005;
    private static final int FLAG_MARK_EN   = 0x006 | LOCKABLE_MASK;
    private static final int FLAG_MARK      = 0x007;
    private static final int FLAG_PROTOCOL  = 0x008;    
    @SuppressWarnings("unused")
    private static final int FLAG_MASK      = 0x0FF | LOCKABLE_MASK;
    @SuppressWarnings("unused")
    private static final int FLAG_SRC       = 0x100;
    private static final int FLAG_SRC_MASK  = 0x100;

    private final MutableEndpoint src;
    private final MutableEndpoint dst;

    /**
     * A lock on values that shouldn't be mutable after a certain point.</p>
     * For instance, you cannot modify the destination IP of a UDP session once the session
     * has started */
    protected boolean locked = false;
    
    /* Pointer to the netcap_pkt_t structure in netcap */
    protected CPointer pointer;
    
    /**
     * Create a blank UDPAttributes
     */
    private UDPAttributes()
    {
        this.src = new UDPAttributesEndpoint( true );
        this.dst = new UDPAttributesEndpoint( false );
    }

    /**
     * Create a blank UDPAttributes
     * @param src
     * @param srcPort
     * @param dst
     * @param dstPort
     */
    public UDPAttributes( InetAddress src, int srcPort, InetAddress dst, int dstPort ) 
    {
        this();

        /* Create a new destination with the other parameters set to their defaults */
        pointer = new CPointer ( createUDPAttributes( Inet4AddressConverter.toLong( src ), srcPort,
                                                      Inet4AddressConverter.toLong( dst ), dstPort ));
    }

    /**
     * Create a blank UDPAttributes
     * @param endpoints
     */
    public UDPAttributes( Endpoints endpoints ) 
    {
        this( endpoints.client().host(), endpoints.client().port(), 
              endpoints.server().host(), endpoints.server().port());
    }

    /**
     * Create a new UDPAttributes with the endpoints with the option of
     * swapping the client and server endpoint.
     * @param endpoints - src and destination.  (client is src, server is dst)
     * @return UDPAttributes
     */
    public static UDPAttributes makeSwapped( Endpoints endpoints )
    {
        return new UDPAttributes( endpoints.server().host(), endpoints.server().port(), 
                                  endpoints.client().host(), endpoints.client().port());        
    }

    /**
     * Create a UDPAttributes with the provided C pointer
     * @param pointer
     */
    protected UDPAttributes( CPointer pointer ) 
    {
        this();
        this.pointer = pointer;
    }

    /**
     * Get the C pointer.
     * @return the pointer
     */
    public long pointer()
    {
        return pointer.value();
    }

    /**
     * Get the source attributes
     * @return the source
     */
    public MutableEndpoint src() 
    {
        return src;
    }

    /**
     * Get the destination attributes
     * @return the destination
     */
    public MutableEndpoint dst() 
    {
        return dst;
    }

    /**
     * Get the ttl attribute
     * @return the ttl
     */
    public byte ttl() 
    {
        return (byte)getIntValue( FLAG_TTL );
    }

    /**
     * Get the tos attribute
     * @return the tos
     */
    public byte tos() 
    {
        return (byte)getIntValue( FLAG_TOS );
    }

    /**
     * Get the protocol (UDP)
     * @return the protocol
     */
    public int getProtocol()
    {
        return getIntValue( FLAG_PROTOCOL );
    }

    /**
     * Return true if MARK setting enabled
     * @return markEnabled
     */
    public boolean isMarkEnabled()
    {
        return ( getIntValue( FLAG_MARK_EN ) == 0 ) ? false : true;
    }

    /**
     * Get the mark attribute
     * @return the mark
     */
    public int mark()
    {
        /**
         * This is called at odd times creating a race condition
         * Catch and ignore any failures (bug #8292)
         */
        try {
            return getIntValue( FLAG_MARK );
        } catch (NullPointerException e) {
            return 0;
        }
    }

    /**
     * Set the ttl attribute
     * @param value - ttl
     */
    public void ttl( byte value )
    {
        setIntValue( FLAG_TTL, value );
    }
    
    /**
     * Set the tos attribute
     * @param value - ttl
     */
    public void tos( byte value )
    {
        setIntValue( FLAG_TOS, value );
    }

    /**
     * Set markEnabled
     * @param isEnabled
     */
    public void isMarkEnabled( boolean isEnabled )
    {
        int value = ( isEnabled ) ? 1 : 0;
        setIntValue( FLAG_MARK_EN, value );
    }

    /**
     * Set the mark
     * @param value
     */
    public void mark( int value )
    {
        setIntValue( FLAG_MARK, value );
    }

    /**
     * Get the srcInterfaceId attribute
     * @return the source interface ID
     */
    public int srcInterfaceId()
    {
        return interfaceId( true );
    }

    /**
     * Get the dstInterfaceId attribute
     * @return the destination interface ID
     */
    public int dstInterfaceId()
    {
        return interfaceId( false );
    }

    /**
     * Set srcInterfaceId.
     * @param id
     */
    public void srcInterfaceId( byte id )
    {
        interfaceId( true, id );
    }

    /**
     * Set dstInterfaceId.
     * @param id
     */
    public void dstInterfaceId( byte id )
    {
        interfaceId( false, id );
    }

    /**
     * interfaceId.
     * @param isSrc
     * @return id
     */
    private int interfaceId( boolean isSrc )
    {
        return getIntValue( buildMask( isSrc, FLAG_INTERFACE ));
    }
    
    /**
     * interfaceId.
     * @param isSrc
     * @param id
     */
    private void interfaceId( boolean isSrc, byte id )
    {
        setIntValue( buildMask( isSrc, FLAG_INTERFACE ), id );
    }

    /**
     * Destroy this object (and call raze() on C object)
     */
    public void raze()
    {
        raze( pointer.value() );
        pointer.raze();
    }

    /**
     * Send this data
     * @param data
     */
    public void send( byte[] data )
    {
        if ( send( pointer.value(), data ) < 0 )
            logger.error("send()");
    }
    
    /**
     * Send this data
     * @param data
     */
    public void send( String data )
    {
        send( data.getBytes());
    }

    /**
     * Set lock=true
     */
    public void lock()
    {
        locked = true;
    }

    /**
     * Generaic getLongValue for getting long based values
     * @param req
     * @return long
     */
    protected long   getLongValue         ( int req )
    { 
        /* How to handle error here ?? */
        return getLongValue( pointer.value(), req );
    }

    /**
     * Generaic getIntValue for getting int based values
     * @param req
     * @return int
     */
    protected int    getIntValue          ( int req )
    {
        int temp = getIntValue( pointer.value(), req );
        if ( temp < 0 ) logger.error( "getIntValue: " + req );
        return temp;
    }

    /**
     * Generate setLongValue for setting long values
     * @param req
     * @param value
     */
    protected void setLongValue ( int req, long value ) 
    {
        checkLock( req );
        if ( setLongValue( pointer.value(), req, value ) < 0 ) logger.error( "setLongValue: " + req );
    }

    /**
     * Generate setIntValue for setting int values
     * @param req
     * @param value
     */
    protected void setIntValue ( int req, int value ) 
    {
        checkLock( req );
        if ( setIntValue( pointer.value(), req, value ) < 0 ) logger.error( "setIntValue: " + req );
    }

    /**
     * checkLock
     * @param req
     */
    protected void checkLock( int req )
    {
        if ( locked && (( req & LOCKABLE_MASK ) == LOCKABLE_MASK )) {
            logger.error( "Attempt to modify a locked value" );
        }
    }

    /**
     * buildMask
     * @param isSrc
     * @param type
     * @return int
     */
    private int buildMask( boolean isSrc, int type )
    {
        return (( isSrc ) ? FLAG_SRC_MASK : 0 ) | type;
    }

    /**
     * Description for createUDPAttributes.
     * @param src
     * @param srcPort
     * @param dst
     * @param dstPort
     * @return
     */
    private static native long   createUDPAttributes ( long src, int srcPort, long dst, int dstPort );
    /**
     * getLongValue.
     * @param packetPointer
     * @param req
     * @return
     */
    private static native long   getLongValue    ( long packetPointer, int req );
    /**
     * getIntValue.
     * @param packetPointer
     * @param req
     * @return
     */
    private static native int    getIntValue     ( long packetPointer, int req );
    /**
     * setLongValue.
     * @param packetPointer
     * @param req
     * @param value
     * @return
     */
    private static native int    setLongValue    ( long packetPointer, int req, long value );
    /**
     * setIntValue.
     * @param packetPointer
     * @param req
     * @param value
     * @return
     */
    private static native int    setIntValue     ( long packetPointer, int req, int value );
    /**
     * send.
     * @param packetPointer
     * @param data
     * @return
     */
    private static native int    send            ( long packetPointer, byte[] data );
    /**
     * raze.
     * @param packetPointer
     */
    private static native void   raze            ( long packetPointer );
    
    /**
     * An individual endpoint attributes
     */
    private class UDPAttributesEndpoint implements MutableEndpoint
    {
        private final boolean ifSrc;

        /**
         * UDPAttributesEndpoint.
         * @param ifSrc
         */
        UDPAttributesEndpoint( boolean ifSrc ) 
        {
            this.ifSrc = ifSrc;
        }
        
        /**
         * host.
         * @return
         */
        public InetAddress host() 
        {
            return Inet4AddressConverter.toAddress( getLongValue( buildMask( FLAG_HOST )));
        }
        
        /**
         * port.
         * @return
         */
        public int port()
        {
            return getIntValue( buildMask( FLAG_PORT ));
        }

        /**
         * host.
         * @param address
         */
        public void host( InetAddress address ) 
        {
            setLongValue( buildMask( FLAG_HOST ), Inet4AddressConverter.toLong( address ));
        }
        
        /**
         * port.
         * @param port
         */
        public void port( int port ) 
        { 
            setIntValue( buildMask( FLAG_PORT ), port );
        }

        /**
         * buildMask.
         * @param type
         * @return
         */
        private int buildMask( int type ) 
        {
            return (( ifSrc ) ? FLAG_SRC_MASK : 0 ) | type;
        }
    }
}
