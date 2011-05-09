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

/* This should probably be more generic, but it doesn't exactly apply for TCP, it is mainly only
 * for ICMP and UDP.  TCP packets died above netcap when we stopped catching SYN/ACKS *
 * All of the methods apply for TCP except for the send method which should only be for UDP traffic
 * objects */
/* XXX This is not a good name */
/* XXX Could potentially be abstract */
public class IPTraffic
{
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
    
    private IPTraffic()
    {
        this.src = new IPTrafficEndpoint( true );
        this.dst = new IPTrafficEndpoint( false );
    }

    public IPTraffic( InetAddress src, int srcPort, InetAddress dst, int dstPort ) 
    {
        this();

        /* Create a new destination with the other parameters set to their defaults */
        pointer = new CPointer ( createIPTraffic( Inet4AddressConverter.toLong( src ), srcPort,
                                                  Inet4AddressConverter.toLong( dst ), dstPort ));
    }

    public IPTraffic( Endpoints endpoints ) 
    {
        this( endpoints.client().host(), endpoints.client().port(), 
              endpoints.server().host(), endpoints.server().port());
    }

    /**
     * Create a new IPTraffic with the endpoints with the option of
     * swapping the client and server endpoint.
     * @param endpoints - src and destination.  (client is src, server is dst)
     * @param swapped - whether or not to swap the endpoints.
     */
    public static IPTraffic makeSwapped( Endpoints endpoints )
    {
        return new IPTraffic( endpoints.server().host(), endpoints.server().port(), 
                              endpoints.client().host(), endpoints.client().port());        
    }

    protected IPTraffic( CPointer pointer ) 
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

    public int protocol()
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

    public String srcInterfaceName()
    {
        return interfaceName( true );
    }

    public String dstInterfaceName()
    {
        return interfaceName( false );
    }

    public byte srcInterfaceId()
    {
        return interfaceId( true );
    }

    public byte dstInterfaceId()
    {
        return interfaceId( false );
    }

    public void srcInterfaceName( String name )
    {
        interfaceName( true, name );
    }

    public void dstInterfaceName( String name )
    {
        interfaceName( false, name );
    }

    public void srcInterfaceId( byte id )
    {
        interfaceId( true, id );
    }

    public void dstInterfaceId( byte id )
    {
        interfaceId( false, id );
    }

    private String interfaceName( boolean isSrc )
    {
        return getStringValue( buildMask( isSrc, FLAG_INTERFACE ));
    }

    private byte interfaceId( boolean isSrc )
    {
        return (byte)getIntValue( buildMask( isSrc, FLAG_INTERFACE ));
    }
    
    private void interfaceName( boolean isSrc, String name ) {
        setStringValue( buildMask( isSrc, FLAG_INTERFACE ), name );
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
            Netcap.error();
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
        if ( temp < 0 ) Netcap.error( "getIntValue: " + req );
        return temp;
    }

    protected String getStringValue       ( int req )
    { 
        String temp = getStringValue( pointer.value(), req );
        if ( temp == null ) Netcap.error( "getStringValue: " + req );
        return temp;
    }

    protected void setLongValue ( int req, long value ) 
    {
        checkLock( req );
        if ( setLongValue( pointer.value(), req, value ) < 0 ) Netcap.error( "setLongValue: " + req );
    }

    protected void setIntValue ( int req, int value ) 
    {
        checkLock( req );
        if ( setIntValue( pointer.value(), req, value ) < 0 ) Netcap.error( "setIntValue: " + req );
    }

    protected void setStringValue ( int req, String value ) 
    {
        checkLock( req );
        if ( setStringValue( pointer.value(), req, value ) < 0 ) Netcap.error( "setStringValue: " + req );
    }

    protected void checkLock( int req )
    {
        if ( locked && (( req & LOCKABLE_MASK ) == LOCKABLE_MASK )) {
            Netcap.error( "Attempt to modify a locked value" );
        }
    }

    private int buildMask( boolean isSrc, int type )
    {
        return (( isSrc ) ? FLAG_SRC_MASK : 0 ) | type;
    }

    static
    {
        Netcap.load();
    }

    /* XXX Consolidate all of the get/set functions into a group of package functions
     * inside of the Netcap class */
    private static native long   createIPTraffic ( long src, int srcPort, long dst, int dstPort );
    private static native long   getLongValue    ( long packetPointer, int req );
    private static native int    getIntValue     ( long packetPointer, int req );
    private static native String getStringValue  ( long packetPointer, int req );
    private static native int    setLongValue    ( long packetPointer, int req, long value );
    private static native int    setIntValue     ( long packetPointer, int req, int value );
    private static native int    setStringValue  ( long packetPointer, int req, String value );
    private static native int    send            ( long packetPointer, byte[] data );
    private static native void   raze            ( long packetPointer );
    
    private class IPTrafficEndpoint implements MutableEndpoint {
        private final boolean ifSrc;

        IPTrafficEndpoint( boolean ifSrc ) 
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
