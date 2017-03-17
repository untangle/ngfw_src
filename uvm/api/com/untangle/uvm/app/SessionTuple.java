/*
 * $Id$
 */
package com.untangle.uvm.app;

import java.net.InetAddress;

/**
 * This is a generic 5-tuple that describes sessions
 * (Protocol, Client, Client Port, Server, Server Port)
 */
public class SessionTuple
{
    public static final short PROTO_TCP = 6;
    public static final short PROTO_UDP = 17;

    private short protocol = 0;
    private InetAddress clientAddr;
    private int clientPort = 0;
    private InetAddress serverAddr;
    private int serverPort = 0;
    
    public SessionTuple( short protocol, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort )
    {
        this.protocol = protocol;
        this.clientAddr = clientAddr;
        this.clientPort = clientPort;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
    }

    public SessionTuple( SessionTuple tuple )
    {
        this.protocol = tuple.getProtocol();
        this.clientAddr = tuple.getClientAddr();
        this.clientPort = tuple.getClientPort();
        this.serverAddr = tuple.getServerAddr();
        this.serverPort = tuple.getServerPort();
    }

    /**
     * Returns the protocol for the session.</p>
     * @return a <code>short</code> giving one of the protocols (right now always TCP(6) or UDP(17))
     */
    public short getProtocol() { return this.protocol; }
    public void setProtocol( short protocol ) { this.protocol = protocol; }

    /**
     * Gets the Client Address of this session. </p>
     *
     * @return  the client address
     */
    public InetAddress getClientAddr() { return this.clientAddr; }
    public void setClientAddr( InetAddress clientAddr ) { this.clientAddr = clientAddr; }

    /**
     * Gets the client port for this session.</p>
     * @return the client port.
     */
    public int getClientPort() { return this.clientPort; }
    public void setClientPort( int clientPort ) { this.clientPort = clientPort; }

    /**
     * Gets the Server Address of this session. </p>
     *
     * @return  the server addr.
     */
    public InetAddress getServerAddr() { return this.serverAddr; }
    public void setServerAddr( InetAddress serverAddr ) { this.serverAddr = serverAddr; }

    /**
     * Gets the server port for this session.</p>
     * @return the server port.
     */
    public int getServerPort() { return this.serverPort; }
    public void setServerPort( int serverPort ) { this.serverPort = serverPort; }

    @Override
    public int hashCode()
    {
        if ( clientAddr == null || serverAddr == null )
            return protocol + clientPort + serverPort;
        else
            return protocol + clientAddr.hashCode() + clientPort + serverAddr.hashCode() + serverPort;
    }

    @Override
    public boolean equals(Object o)
    {
        if ( o == null )
            return false;
        if ( ! (o instanceof SessionTuple) )
            return false;
        SessionTuple t = (SessionTuple)o;
        if ( t.protocol != this.protocol || t.clientPort != this.clientPort || t.serverPort != this.serverPort)
            return false;
        if ( ! ( t.clientAddr == null ? this.clientAddr == null : t.clientAddr.equals(this.clientAddr) ) )
            return false;
        if ( ! ( t.serverAddr == null ? this.serverAddr == null : t.serverAddr.equals(this.serverAddr) ) )
            return false;
        return true;
    }


    @Override
    public String toString()
    {
        String str = "[Tuple ";
        switch ( protocol ) {
        case PROTO_UDP:
            str += "UDP ";
            break;
        case PROTO_TCP:
            str += "TCP ";
            break;
        default:
            str += "PROTO:" + protocol + " ";
            break;
        }
        str += (clientAddr == null ? "null" : clientAddr.getHostAddress()) + ":" + clientPort;
        str += " -> ";
        str += (serverAddr == null ? "null" : serverAddr.getHostAddress()) + ":" + serverPort;
        str += "]";
        
        return str;
    }
}
