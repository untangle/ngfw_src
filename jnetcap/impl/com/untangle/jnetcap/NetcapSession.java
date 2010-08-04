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

public abstract class NetcapSession
{
    /* Pointer to the netcap_session_t structure in netcap */
    protected CPointer pointer;

    private final static int FLAG_ID         = 1;
    private final static int FLAG_PROTOCOL   = 2;
    private final static int FLAG_ICMP_MB    = 3;

    protected final static int FLAG_IF_CLIENT_MASK = 0x2000;
    protected final static int FLAG_IF_SRC_MASK    = 0x1000;

    /* For the following options one of FLAG_ClientMask or FLAG_ServerMask AND FLAG_SrcMask or FLAG_DstMask,
     * must also be set */
    private final static int FLAG_HOST          = 16;
    private final static int FLAG_PORT          = 17;
    private final static int FLAG_INTERFACE     = 18;
    private final static int FLAG_NAT_FROM_HOST = 19;
    private final static int FLAG_NAT_FROM_PORT = 20;
    private final static int FLAG_NAT_TO_HOST   = 21;
    private final static int FLAG_NAT_TO_PORT   = 22;

    /* This is the mask for the remove the client/server parts */
    @SuppressWarnings("unused")
	private final static int FLAG_MASK        = 0xFFF;

    protected final Endpoints clientSide;
    protected final Endpoints serverSide;
    
    final ICMPMailbox icmpClientMailbox;
    final ICMPMailbox icmpServerMailbox;

    public NatInfo natInfo;


    /* This is for children that override the SessionEndpoints class */
    protected NetcapSession( int id, short protocol )
    {
        pointer = new CPointer( getSession( id, protocol ));

        clientSide = makeEndpoints( true );
        serverSide = makeEndpoints( false );
     	
        /* Create the server and client ICMP mailboxes */
        icmpClientMailbox = new ICMPMailbox( new CPointer( icmpMailbox( true )));
        icmpServerMailbox = new ICMPMailbox( new CPointer( icmpMailbox( false )));
        updateNatInfo();
    }
    
    /* Returns one of Netcap.IPPROTO_UDP, Netcap.IPPROTO_TCP, Netcap.IPPROTO_ICMP */
    public short protocol()
    {
        short protocol = (short)getIntValue( FLAG_PROTOCOL, pointer.value());
        
        /* XXX ICMP Hack, the protocol switch to ICMP has only occured on the C-side */
        if ( protocol == Netcap.IPPROTO_ICMP ) protocol = Netcap.IPPROTO_UDP;

        return protocol;
    }

    public int id() 
    {
        return getIntValue( FLAG_ID, pointer.value());
    }

    private long icmpMailbox( boolean isClient )
    {
        int flag = FLAG_ICMP_MB | (( isClient ) ? FLAG_IF_CLIENT_MASK : 0 );
        return getLongValue( flag, pointer.value());
    }
    
    public ICMPMailbox icmpClientMailbox()
    {
        return icmpClientMailbox;
    }

    public ICMPMailbox icmpServerMailbox()
    {
        return icmpServerMailbox;
    }
    
    public String toString( boolean ifClient )
    {
        return toString( pointer.value(), ifClient );
    }

    public String toString()
    {
        return toString( pointer.value(), true );
    }

    public int  clientMark()
    {
        return getClientMark( pointer.value() );
    }
    
    public void clientMark(int newmark)
    {
        setClientMark( pointer.value(), newmark );
    }

    public int  serverMark()
    {
        return getServerMark( pointer.value() );
    }
    
    public void serverMark(int newmark)
    {
        setServerMark( pointer.value(), newmark );
    }

    public void determineServerIntf( boolean isSingleNicMode )
    {
        determineServerIntf( pointer.value(), isSingleNicMode );
    }

    public void raze()
    {
        raze( pointer.value());

        pointer.raze();
    }

    private void updateNatInfo()
    {
        natInfo = new NatInfo();
        natInfo.fromHost = Inet4AddressConverter.toAddress(getLongValue( FLAG_NAT_FROM_HOST, pointer.value()));
        natInfo.fromPort = getIntValue( FLAG_NAT_FROM_PORT, pointer.value());
        natInfo.toHost   = Inet4AddressConverter.toAddress(getLongValue( FLAG_NAT_TO_HOST, pointer.value()));
        natInfo.toPort   = getIntValue( FLAG_NAT_TO_PORT, pointer.value());
    }

    protected abstract Endpoints makeEndpoints( boolean ifClient );

    public Endpoints clientSide() { return clientSide; }
    public Endpoints serverSide() { return serverSide; }

    private static native long getSession( int id, short protocol );
    private static native void raze( long session );
    private static native void determineServerIntf( long session, boolean isSingleNicMode );

    private static native int  getClientMark( long session );
    private static native void setClientMark( long session, int mark );
    private static native int  getServerMark( long session );
    private static native void setServerMark( long session, int mark );

    protected static native long   getLongValue  ( int id, long session );
    protected static native int    getIntValue   ( int id, long session );
    protected static native String getStringValue( int id, long session );
    protected static native String toString( long session, boolean ifClient );

    static
    {
        Netcap.load();
    }

    public class NatInfo 
    {
        public InetAddress fromHost;
        public int fromPort;
        public InetAddress toHost;
        public int toPort;
    }

    protected class SessionEndpoints implements Endpoints
    {
        protected final boolean ifClientSide;
        protected final Endpoint client;
        protected final Endpoint server;

        SessionEndpoints( boolean ifClientSide ) {
            this.ifClientSide = ifClientSide;
            client = new SessionEndpoint( true );
            server = new SessionEndpoint( false );
        }

        public Endpoint client() { return client; }
        public Endpoint server() { return server; }

        public String interfaceName()
        {
            return getStringValue( buildMask( FLAG_INTERFACE ), pointer.value());
        }
        
        public byte interfaceId()
        {
            return (byte)getIntValue( buildMask( FLAG_INTERFACE ), pointer.value());
        }

        protected int buildMask( int type )
        {
            return (( ifClientSide ) ? FLAG_IF_CLIENT_MASK : 0) | type;
        }

        protected class SessionEndpoint implements Endpoint {
            private final boolean ifClient;

            SessionEndpoint( boolean ifClient )
            {
                this.ifClient = ifClient;
            }

            public InetAddress host()
            {
                long addr = getLongValue( buildMask( FLAG_HOST ), pointer.value());

                return Inet4AddressConverter.toAddress( addr );
            }

            public int port()
            {
                return getIntValue( buildMask( FLAG_PORT ), pointer.value());
            }

            protected int buildMask( int type )
            {
                int mask = ( ifClientSide ) ? FLAG_IF_CLIENT_MASK : 0;
                mask |= ( ifClient ) ? FLAG_IF_SRC_MASK : 0;
                mask |= type;
                return mask;
            }
        }
    }
}
