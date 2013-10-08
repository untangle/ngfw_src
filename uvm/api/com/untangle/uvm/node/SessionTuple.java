/*
 * $Id: SessionTuple.java 34506 2013-04-08 23:04:30Z dmorris $
 */
package com.untangle.uvm.node;

import java.net.InetAddress;

/**
 * This is a generic 7-tuple that describes sessions
 * (Protocol, Client Intf, Client, Client Port, Server Intf, Server, Server Port)
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
     * Returns an netcap interface for the client.</p>
     *
     * @return a <code>int</code> giving the client interface of the session.
     */
    int getClientIntf();

    /**
     * Returns an netcap interface for the server.</p>
     *
     * @return a <code>int</code> giving the server interface of the session.
     */
    int getServerIntf();

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

    /**
     * Return the session ID
     */
    long getSessionId();
}
