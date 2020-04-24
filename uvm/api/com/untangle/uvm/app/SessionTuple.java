/**
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
    private int srcInterface = 0;

    /**
     * Constructor
     * @param protocol The protocol
     * @param clientAddr The client address
     * @param serverAddr The server address
     * @param clientPort The client port
     * @param serverPort The server port
     * @param srcInterface The source interface
     */
    public SessionTuple( short protocol, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort, int srcInterface )
    {
        this.protocol = protocol;
        this.clientAddr = clientAddr;
        this.clientPort = clientPort;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.srcInterface = srcInterface;
    }

    /**
     * Constructor
     * @param tuple The tuple to copy
     */
    public SessionTuple( SessionTuple tuple )
    {
        this.protocol = tuple.getProtocol();
        this.clientAddr = tuple.getClientAddr();
        this.clientPort = tuple.getClientPort();
        this.serverAddr = tuple.getServerAddr();
        this.serverPort = tuple.getServerPort();
        this.srcInterface = tuple.getSrcInterface();
    }

    /**
     * Returns the protocol for the session.</p>
     * @return a <code>short</code> giving one of the protocols (right now always TCP(6) or UDP(17))
     */
    public short getProtocol() { return this.protocol; }
    
    /**
     * Set the protocol
     * @param protocol The protocol
     */
    public void setProtocol( short protocol ) { this.protocol = protocol; }

    /**
     * Gets the Client Address of this session. </p>
     *
     * @return  the client address
     */
    public InetAddress getClientAddr() { return this.clientAddr; }
    
    /**
     * Set the client address
     * @param clientAddr The client address
     */
    public void setClientAddr( InetAddress clientAddr ) { this.clientAddr = clientAddr; }

    /**
     * Gets the client port for this session.</p>
     * @return the client port.
     */
    public int getClientPort() { return this.clientPort; }
    
    /**
     * Set the client port
     * @param clientPort The client port
     */
    public void setClientPort( int clientPort ) { this.clientPort = clientPort; }

    /**
     * Gets the Server Address of this session. </p>
     *
     * @return  the server addr.
     */
    public InetAddress getServerAddr() { return this.serverAddr; }
    
    /**
     * Set the server address
     * @param serverAddr The server address
     */
    public void setServerAddr( InetAddress serverAddr ) { this.serverAddr = serverAddr; }

    /**
     * Gets the server port for this session.</p>
     * @return the server port.
     */
    public int getServerPort() { return this.serverPort; }

    /**
     * Set the server port
     * @param serverPort The server port
     */
    public void setServerPort( int serverPort ) { this.serverPort = serverPort; }

    /**
     * Get the source interface
     * @return the source interface
     */
    public int getSrcInterface() { return this.srcInterface; }

    /**
     * Set the source interface
     * @param srcInterface The source interface
     */
    public void setSrcInterface(int srcInterface) { this.srcInterface = srcInterface; }

    /**
     * Get the hash code
     * @return The hash code
     */
    @Override
    public int hashCode()
    {
        if ( clientAddr == null || serverAddr == null )
            return protocol + clientPort + serverPort + srcInterface;
        else
            return protocol + clientAddr.hashCode() + clientPort + serverAddr.hashCode() + serverPort + srcInterface;
    }

    /**
     * Compare to another object
     * @param o The object for comparison
     * @return True if equal, otherwise false
     */
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
        if ( t.srcInterface != this.srcInterface )
            return false;
        return true;
    }

    /**
     * Get the string representation
     * @return The string representation
     */
    @Override
    public String toString()
    {
        String str = "[Tuple ";
        str += "{" + Integer.toString(srcInterface) + "} ";

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
