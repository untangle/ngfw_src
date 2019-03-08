/**
 * $Id$
 */
package com.untangle.jnetcap;

import java.net.InetAddress;

/**
 * NetcapSession
 */
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
    
    /**
     * This is for children that override the SessionEndpoints class
     * @param id - the ID
     */
    protected NetcapSession( long id )
    {
        pointer = new CPointer( getSession( id ));

        clientSide = makeEndpoints( true );
        serverSide = makeEndpoints( false );
    }
    
    /**
     * getProtocol gets the protocol
     * Returns one of Netcap.IPPROTO_UDP, Netcap.IPPROTO_TCP
     * @return the protocol
     */
    public short getProtocol()
    {
        return (short)getIntValue( FLAG_PROTOCOL, pointer.value());
    }

    /**
     * id gets the ID
     * @return id
     */
    public long id() 
    {
        return getLongValue( FLAG_ID, pointer.value());
    }

    /**
     * toString
     * @param ifClient
     * @return string
     */
    public String toString( boolean ifClient )
    {
        return toString( pointer.value(), ifClient );
    }

    /**
     * toString - cass the C string function
     * @return string
     */
    public String toString()
    {
        return toString( pointer.value(), true );
    }

    /**
     * clientMark gets the client side mark
     * @return the client mark
     */
    public int  clientMark()
    {
        try {
            return getClientMark( pointer.value() );
        } catch (NullPointerException e) {
            return 0; /* pointer is gone, session dead */
        }
    }
    
    /**
     * clientQosMark - gets the client side QoS mark (priority)
     * @return the priority
     */
    public int  clientQosMark()
    {
        //QoSMask = 0x000F0000 (16 bits to the left)
        return ((clientMark() & 0x000F0000) >> 16);
    }

    /**
     * clientMark sets the client-side mark
     * @param newmark 
     */
    public void clientMark(int newmark)
    {
        setClientMark( pointer.value(), newmark );
    }

    /**
     * orClientMark
     * @param bitmask 
     */
    public void orClientMark(int bitmask)
    {
        int orig_client_mark = this.clientMark();
        int client_mark = orig_client_mark | bitmask;

        if (client_mark == orig_client_mark)
            return;
        
        this.clientMark(client_mark);
    }
    
    /**
     * clientQosMark - sets the QoS mark of the client side
     * @param priority (0-8)
     */
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

    /**
     * serverMark - gets the server side mark
     * @return the server side mark
     */
    public int  serverMark()
    {
        try {
            return getServerMark( pointer.value() );
        } catch (NullPointerException e) {
            return 0; /* pointer is gone, session dead */
        }
    }

    /**
     * serverQosMark - get the server QoS mark (priority)
     * @return the priority
     */
    public int  serverQosMark()
    {
        //QoSMask = 0x000F0000 (16 bits to the left)
        return ((serverMark() & 0x000F0000) >> 16);
    }
    
    /**
     * serverMark - set the server side mark to the specified value
     * @param newmark 
     */
    public void serverMark(int newmark)
    {
        setServerMark( pointer.value(), newmark );
    }

    /**
     * orServerMark - or the existing mark with the specified value
     * @param bitmask - the value
     */
    public void orServerMark(int bitmask)
    {
        int orig_server_mark = this.serverMark();
        int server_mark = orig_server_mark | bitmask;

        if (server_mark == orig_server_mark)
            return;
        
        this.serverMark(server_mark);
    }
    
    /**
     * serverQosMark sets the server-side QoS mark
     * @param priority (0-8)
     */
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
    
    /**
     * setServerIntf sets the server-side interface ID
     * @param intf - the interface ID
     */
    public void setServerIntf(int intf)
    {
        setServerIntf( pointer.value(), intf );
    }

    /**
     * raze/free this session
     */
    public void raze()
    {
        raze( pointer.value());

        pointer.raze();
    }

    /**
     * makeEndpoints
     * @param ifClient - true if client side, false if server side
     * @return the new Endpoints
     */
    protected abstract Endpoints makeEndpoints( boolean ifClient );

    /**
     * clientSide
     * @return the client side Endpoints
     */
    public Endpoints clientSide()
    {
        return clientSide;
    }

    /**
     * serverSide
     * @return the server side Endpoints
     */
    public Endpoints serverSide()
    {
        return serverSide;
    }

    /**
     * getSession
     * @param id 
     * @return 
     */
    private static native long getSession( long id );

    /**
     * raze
     * @param session 
     */
    private static native void raze( long session );

    /**
     * getClientMark
     * @param session 
     * @return 
     */
    private static native int  getClientMark( long session );

    /**
     * setClientMark
     * @param session 
     * @param mark 
     */
    private static native void setClientMark( long session, int mark );

    /**
     * getServerMark
     * @param session 
     * @return 
     */
    private static native int  getServerMark( long session );

    /**
     * setServerMark
     * @param session 
     * @param mark 
     */
    private static native void setServerMark( long session, int mark );

    /**
     * setServerIntf
     * @param session 
     * @param mark 
     */
    private static native void setServerIntf( long session, int mark );
    
    /**
     * getLongValue
     * @param id 
     * @param session 
     * @return 
     */
    protected static native long   getLongValue  ( int id, long session );

    /**
     * getIntValue
     * @param id 
     * @param session 
     * @return 
     */
    protected static native int    getIntValue   ( int id, long session );

    /**
     * toString
     * @param session 
     * @param ifClient 
     * @return 
     */
    protected static native String toString( long session, boolean ifClient );

    /**
     * SessionEndpoints
     */
    protected class SessionEndpoints implements Endpoints
    {
        protected final boolean isClientSide;
        protected final Endpoint client;
        protected final Endpoint server;

        /**
         * SessionEndpoints
         * @param isClientSide
         */
        SessionEndpoints( boolean isClientSide )
        {
            this.isClientSide = isClientSide;
            client = new SessionEndpoint( true );
            server = new SessionEndpoint( false );
        }

        /**
         * client
         * @return the client endpoint
         */
        public Endpoint client()
        {
            return client;
        }

        /**
         * server
         * @return the server endpoint
         */
        public Endpoint server()
        {
            return server;
        }

        /**
         * interfaceId gets the interface ID of a session endpoint
         * @return the interface ID 
         */
        public int interfaceId()
        {
            if ( isClientSide )
                return getIntValue( FLAG_CLIENT_INTERFACE, pointer.value());
            else
                return getIntValue( FLAG_SERVER_INTERFACE, pointer.value());
        }

        /**
         * SessionEndpoint
         */
        protected class SessionEndpoint implements Endpoint
        {
            private final boolean isClient;

            /**
             * SessionEndpoint - a single session endpoint
             * @param isClient true if client, false if server
             */
            SessionEndpoint( boolean isClient )
            {
                this.isClient = isClient;
            }

            /**
             * host
             * @return the host
             */
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

            /**
             * port
             * @return the port
             */
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
