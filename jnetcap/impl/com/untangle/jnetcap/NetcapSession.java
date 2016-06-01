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
    private final static int FLAG_CLIENTSIDE_CLIENT_HOST = 16;
    private final static int FLAG_CLIENTSIDE_SERVER_HOST = 17;
    private final static int FLAG_SERVERSIDE_CLIENT_HOST = 18;
    private final static int FLAG_SERVERSIDE_SERVER_HOST = 19;
    private final static int FLAG_CLIENTSIDE_CLIENT_PORT = 20;
    private final static int FLAG_CLIENTSIDE_SERVER_PORT = 21;
    private final static int FLAG_SERVERSIDE_CLIENT_PORT = 22;
    private final static int FLAG_SERVERSIDE_SERVER_PORT = 23;
    private final static int FLAG_CLIENT_INTERFACE     = 24;
    private final static int FLAG_SERVER_INTERFACE     = 25;

    protected final Endpoints clientSide;
    protected final Endpoints serverSide;
    
    /* This is for children that override the SessionEndpoints class */
    protected NetcapSession( long id )
    {
        pointer = new CPointer( getSession( id ));

        clientSide = makeEndpoints( true );
        serverSide = makeEndpoints( false );
    }
    
    /* Returns one of Netcap.IPPROTO_UDP, Netcap.IPPROTO_TCP */
    public short getProtocol()
    {
        return (short)getIntValue( FLAG_PROTOCOL, pointer.value());
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
        //QoSMask = 0x000F0000 (16 bits to the left)
        return ((clientMark() & 0x000F0000) >> 16);
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

        //QoSMask   = 0x000f0000 (16 bits to the left)
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
        //QoSMask = 0x000F0000 (16 bits to the left)
        return ((serverMark() & 0x000F0000) >> 16);
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

        //QoSMask   = 0x000F0000 (16 bits to the left)
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

    protected abstract Endpoints makeEndpoints( boolean ifClient );

    public Endpoints clientSide() { return clientSide; }
    public Endpoints serverSide() { return serverSide; }

    private static native long getSession( long id );
    private static native void raze( long session );

    private static native int  getClientMark( long session );
    private static native void setClientMark( long session, int mark );
    private static native int  getServerMark( long session );
    private static native void setServerMark( long session, int mark );

    private static native void setServerIntf( long session, int mark );
    
    protected static native long   getLongValue  ( int id, long session );
    protected static native int    getIntValue   ( int id, long session );
    protected static native String toString( long session, boolean ifClient );

    protected class SessionEndpoints implements Endpoints
    {
        protected final boolean isClientSide;
        protected final Endpoint client;
        protected final Endpoint server;

        SessionEndpoints( boolean isClientSide ) {
            this.isClientSide = isClientSide;
            client = new SessionEndpoint( true );
            server = new SessionEndpoint( false );
        }

        public Endpoint client() { return client; }
        public Endpoint server() { return server; }

        public int interfaceId()
        {
            if ( isClientSide )
                return getIntValue( FLAG_CLIENT_INTERFACE, pointer.value());
            else
                return getIntValue( FLAG_SERVER_INTERFACE, pointer.value());
        }

        protected class SessionEndpoint implements Endpoint
        {
            private final boolean isClient;

            SessionEndpoint( boolean isClient )
            {
                this.isClient = isClient;
            }

            public InetAddress host()
            {
                long addr;
                if ( isClientSide ) {
                    if ( isClient )
                        addr = getLongValue( FLAG_CLIENTSIDE_CLIENT_HOST, pointer.value());
                    else
                        addr = getLongValue( FLAG_CLIENTSIDE_SERVER_HOST, pointer.value());
                } else {
                    if ( isClient )
                        addr = getLongValue( FLAG_SERVERSIDE_CLIENT_HOST, pointer.value());
                    else
                        addr = getLongValue( FLAG_SERVERSIDE_SERVER_HOST, pointer.value());
                }
                    
                return Inet4AddressConverter.toAddress( addr );
            }

            public int port()
            {
                if ( isClientSide ) {
                    if ( isClient )
                        return getIntValue( FLAG_CLIENTSIDE_CLIENT_PORT, pointer.value());
                    else
                        return getIntValue( FLAG_CLIENTSIDE_SERVER_PORT, pointer.value());
                } else {
                    if ( isClient )
                        return getIntValue( FLAG_SERVERSIDE_CLIENT_PORT, pointer.value());
                    else
                        return getIntValue( FLAG_SERVERSIDE_SERVER_PORT, pointer.value());
                }
            }
        }
    }
}
