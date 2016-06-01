/**
 * $Id$
 */
package com.untangle.jnetcap;

import java.net.InetAddress;

import org.apache.log4j.Logger;

/**
 * Represents the attributes of the side (client side or server side) of a UDP session
 */
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
    
    private UDPAttributes()
    {
        this.src = new UDPAttributesEndpoint( true );
        this.dst = new UDPAttributesEndpoint( false );
    }

    public UDPAttributes( InetAddress src, int srcPort, InetAddress dst, int dstPort ) 
    {
        this();

        /* Create a new destination with the other parameters set to their defaults */
        pointer = new CPointer ( createUDPAttributes( Inet4AddressConverter.toLong( src ), srcPort,
                                                Inet4AddressConverter.toLong( dst ), dstPort ));
    }

    public UDPAttributes( Endpoints endpoints ) 
    {
        this( endpoints.client().host(), endpoints.client().port(), 
              endpoints.server().host(), endpoints.server().port());
    }

    /**
     * Create a new UDPAttributes with the endpoints with the option of
     * swapping the client and server endpoint.
     * @param endpoints - src and destination.  (client is src, server is dst)
     * @param swapped - whether or not to swap the endpoints.
     */
    public static UDPAttributes makeSwapped( Endpoints endpoints )
    {
        return new UDPAttributes( endpoints.server().host(), endpoints.server().port(), 
                              endpoints.client().host(), endpoints.client().port());        
    }

    protected UDPAttributes( CPointer pointer ) 
    {
        this();
        this.pointer = pointer;
    }

    public long pointer() { return pointer.value(); }

    public MutableEndpoint src() 
    { 
        return src;
    }

    public MutableEndpoint dst() 
    { 
        return dst;
    }

    /* XXX Create helper get methods that do error checking */
    public byte ttl() 
    {
        return (byte)getIntValue( FLAG_TTL );
    }

    public byte tos() 
    {
        return (byte)getIntValue( FLAG_TOS );
    }

    public int getProtocol()
    {
        return getIntValue( FLAG_PROTOCOL );
    }

    public boolean isMarkEnabled()
    {
        return ( getIntValue( FLAG_MARK_EN ) == 0 ) ? false : true;
    }

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

    public void ttl( byte value )
    { 
        setIntValue( FLAG_TTL, value );
    }
    
    public void tos( byte value )
    {
        setIntValue( FLAG_TOS, value );
    }

    public void isMarkEnabled( boolean isEnabled )
    {
        int value = ( isEnabled ) ? 1 : 0;
        setIntValue( FLAG_MARK_EN, value );
    }

    public void mark( int value )
    {
        setIntValue( FLAG_MARK, value );
    }

    public int srcInterfaceId()
    {
        return interfaceId( true );
    }

    public int dstInterfaceId()
    {
        return interfaceId( false );
    }

    public void srcInterfaceId( byte id )
    {
        interfaceId( true, id );
    }

    public void dstInterfaceId( byte id )
    {
        interfaceId( false, id );
    }

    private int interfaceId( boolean isSrc )
    {
        return getIntValue( buildMask( isSrc, FLAG_INTERFACE ));
    }
    
    private void interfaceId( boolean isSrc, byte id )
    {
        setIntValue( buildMask( isSrc, FLAG_INTERFACE ), id );
    }

    public void raze()
    { 
        raze( pointer.value() );
        pointer.raze();
    }

    public void send( byte[] data )
    {
        if ( send( pointer.value(), data ) < 0 )
            logger.error("send()");
    }
    
    public void send( String data )
    {
        send( data.getBytes());
    }

    public void lock()
    {
        locked = true;
    }

    protected long   getLongValue         ( int req )
    { 
        /* How to handle error here ?? */
        return getLongValue( pointer.value(), req );
    }

    protected int    getIntValue          ( int req )
    {
        int temp = getIntValue( pointer.value(), req );
        if ( temp < 0 ) logger.error( "getIntValue: " + req );
        return temp;
    }

    protected void setLongValue ( int req, long value ) 
    {
        checkLock( req );
        if ( setLongValue( pointer.value(), req, value ) < 0 ) logger.error( "setLongValue: " + req );
    }

    protected void setIntValue ( int req, int value ) 
    {
        checkLock( req );
        if ( setIntValue( pointer.value(), req, value ) < 0 ) logger.error( "setIntValue: " + req );
    }

    protected void checkLock( int req )
    {
        if ( locked && (( req & LOCKABLE_MASK ) == LOCKABLE_MASK )) {
            logger.error( "Attempt to modify a locked value" );
        }
    }

    private int buildMask( boolean isSrc, int type )
    {
        return (( isSrc ) ? FLAG_SRC_MASK : 0 ) | type;
    }

    private static native long   createUDPAttributes ( long src, int srcPort, long dst, int dstPort );
    private static native long   getLongValue    ( long packetPointer, int req );
    private static native int    getIntValue     ( long packetPointer, int req );
    private static native int    setLongValue    ( long packetPointer, int req, long value );
    private static native int    setIntValue     ( long packetPointer, int req, int value );
    private static native int    send            ( long packetPointer, byte[] data );
    private static native void   raze            ( long packetPointer );
    
    private class UDPAttributesEndpoint implements MutableEndpoint
    {
        private final boolean ifSrc;

        UDPAttributesEndpoint( boolean ifSrc ) 
        {
            this.ifSrc = ifSrc;
        }
        
        public InetAddress host() 
        {
            return Inet4AddressConverter.toAddress( getLongValue( buildMask( FLAG_HOST )));
        }
        
        public int port()
        {
            return getIntValue( buildMask( FLAG_PORT ));
        }

        public void host( InetAddress address ) 
        {
            setLongValue( buildMask( FLAG_HOST ), Inet4AddressConverter.toLong( address ));
        }
        
        public void port( int port ) 
        { 
            setIntValue( buildMask( FLAG_PORT ), port );
        }

        private int buildMask( int type ) 
        {
            return (( ifSrc ) ? FLAG_SRC_MASK : 0 ) | type;
        }
    }
}
