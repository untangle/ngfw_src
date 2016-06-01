/*
 * $Id$
 */
package com.untangle.uvm.node;

import java.net.InetAddress;

/**
 * This is a generic 5-tuple that describes sessions
 * (Protocol, Client, Client Port, Server, Server Port)
 */
public interface SessionTuple
{
    public static final short PROTO_TCP = 6;
    public static final short PROTO_UDP = 17;

    /**
     * Returns the protocol for the session.</p>
     * @return a <code>short</code> giving one of the protocols (right now always TCP(6) or UDP(17))
     */
    short getProtocol();

    /**
     * Gets the Client Address of this session. </p>
     *
     * @return  the client address
     */
    InetAddress getClientAddr();

    /**
     * Gets the Server Address of this session. </p>
     *
     * @return  the server addr.
     */
    InetAddress getServerAddr();

    /**
     * Gets the client port for this session.</p>
     * @return the client port.
     */
    int getClientPort();

    /**
     * Gets the server port for this session.</p>
     * @return the server port.
     */
    int getServerPort();
}
