/**
 * $Id$
 */
package com.untangle.jnetcap;

import java.net.InetAddress;

public abstract class NetcapSession
{
    /* Pointer to the netcap_session_t structure in netcap */
    protected CPointer pointer;

    private final static int FLAG_ID         = 1;
    private final static int FLAG_PROTOCOL   = 2;

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

    /* This is the mask for the the client/server parts */
    @SuppressWarnings("unused")
    /* It is suppressed warning here because its used in JNI */
	private final static int FLAG_MASK        = 0xFFF;

    protected final Endpoints clientSide;
    protected final Endpoints serverSide;
    
    public NatInfo natInfo;


    /* This is for children that override the SessionEndpoints class */
    protected NetcapSession( long id, short protocol )
    {
        pointer = new CPointer( getSession( id, protocol ));

        clientSide = makeEndpoints( true );
        serverSide = makeEndpoints( false );
     	
        updateNatInfo();
    }
    
    /* Returns one of Netcap.IPPROTO_UDP, Netcap.IPPROTO_TCP */
    public short getProtocol()
    {
        short protocol = (short)getIntValue( FLAG_PROTOCOL, pointer.value());
        
        return protocol;
    }

    public long id() 
    {
        return getLongValue( FLAG_ID, pointer.value());
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
        try {
            return getClientMark( pointer.value() );
        } catch (NullPointerException e) {
            return 0; /* pointer is gone, session dead */
        }
    }
    
    public int  clientQosMark()
    {
        //QoSMask = 0x00080000 (16 bits to the left)
        return ((clientMark() & 0x00080000) >> 16);
    }

    public void clientMark(int newmark)
    {
        setClientMark( pointer.value(), newmark );
    }

    public void orClientMark(int bitmask)
    {
        int orig_client_mark = this.clientMark();
        int client_mark = orig_client_mark | bitmask;

        if (client_mark == orig_client_mark)
            return;
        
        this.clientMark(client_mark);
    }
    
    public void clientQosMark(int priority)
    {
        if (priority > 7) {
            System.err.println("ERROR: Invalid priority: " + priority);
            return;
        }

        //QoSMask   = 0x00080000 (16 bits to the left)
        //UnQoSMask = 0xfff0ffff 
            
        int orig_client_mark = this.clientMark() & 0xfff0ffff;
        int client_mark = orig_client_mark | (priority << 16);
        
        if (client_mark == orig_client_mark)
            return;

        this.clientMark(client_mark);
    }

    public int  serverMark()
    {
        try {
            return getServerMark( pointer.value() );
        } catch (NullPointerException e) {
            return 0; /* pointer is gone, session dead */
        }
    }

    public int  serverQosMark()
    {
        //QoSMask = 0x00080000 (16 bits to the left)
        return ((serverMark() & 0x00080000) >> 16);
    }
    
    public void serverMark(int newmark)
    {
        setServerMark( pointer.value(), newmark );
    }

    public void orServerMark(int bitmask)
    {
        int orig_server_mark = this.serverMark();
        int server_mark = orig_server_mark | bitmask;

        if (server_mark == orig_server_mark)
            return;
        
        this.serverMark(server_mark);
    }
    
    public void serverQosMark(int priority)
    {
        if (priority > 7) {
            System.err.println("ERROR: Invalid priority: " + priority);
            return;
        }

        //QoSMask   = 0x00080000 (16 bits to the left)
        //UnQoSMask = 0xfff0ffff 
            
        int orig_server_mark = this.serverMark() & 0xfff0ffff;
        int server_mark = orig_server_mark | (priority << 16);
        
        if (server_mark == orig_server_mark)
            return;

        this.serverMark(server_mark);
    }
    
    public void setServerIntf(int intf)
    {
        setServerIntf( pointer.value(), intf );
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

    private static native long getSession( long id, short protocol );
    private static native void raze( long session );

    private static native int  getClientMark( long session );
    private static native void setClientMark( long session, int mark );
    private static native int  getServerMark( long session );
    private static native void setServerMark( long session, int mark );

    private static native void setServerIntf( long session, int mark );
    
    protected static native long   getLongValue  ( int id, long session );
    protected static native int    getIntValue   ( int id, long session );
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

        public int interfaceId()
        {
            return getIntValue( buildMask( FLAG_INTERFACE ), pointer.value());
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
