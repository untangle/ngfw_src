/*
 * $Id$
 */
package com.untangle.uvm.node;

import java.net.InetAddress;

/**
 * Gives information about a sessions endpoints.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface SessionEndpoints
{
    public static final short PROTO_TCP = 6;
    public static final short PROTO_UDP = 17;

    /**
     * Returns the protocol for the session.</p>
     * @return a <code>short</code> giving one of the protocols (right now always TCP or UDP)
     */
    short protocol();

    /**
     * Returns an argon interface for the client.</p>
     *
     * @return a <code>int</code> giving the client interface of the session.
     */
    int clientIntf();

    /**
     * Returns an argon interface for the server.</p>
     *
     * @return a <code>int</code> giving the server interface of the session.
     */
    int serverIntf();

    /**
     * Gets the Client Address of this session. </p>
     *
     * @return  the client address
     */
    InetAddress clientAddr();

    /**
     * Gets the Server Address of this session. </p>
     *
     * @return  the server addr.
     */
    InetAddress serverAddr();

    /**
     * Gets the client port for this session.</p>
     * @return the client port.
     */
    int clientPort();

    /**
     * Gets the server port for this session.</p>
     * @return the server port.
     */
    int serverPort();
}
