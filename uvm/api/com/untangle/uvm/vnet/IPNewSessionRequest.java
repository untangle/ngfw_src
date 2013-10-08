/**
 * $Id: IPNewSessionRequest.java 34444 2013-04-01 23:17:13Z dmorris $
 */
package com.untangle.uvm.vnet;

import java.net.InetAddress;

import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.node.SessionTuple;

/**
 * The new IP session request interface
 */
public interface IPNewSessionRequest extends NewSessionRequest, SessionTuple
{
    /**
     * Sets the client address for this session.
     */
    void setClientAddr( InetAddress addr );

    /**
     * Sets the client port for this session.
     */
    void setClientPort( int port );

    /**
     * Sets the server address for this session.
     */
    void setServerAddr( InetAddress addr );

    /**
     * Sets the server port for this session.
     */
    void setServerPort( int port );

    /**
     * Get the Session Event for this session
     */
    SessionEvent sessionEvent();

    /**
     * May only be called before session is established (from SessionRequest handler)
     */
    void rejectSilently();


    // Codes for rejectReturnUnreachable()
    static final byte NET_UNREACHABLE = 0;
    static final byte HOST_UNREACHABLE = 1;
    static final byte PROTOCOL_UNREACHABLE = 2;
    static final byte PORT_UNREACHABLE = 3;
    // static final byte DEST_NETWORK_UNKNOWN = 6;  // By RFC1812, should use NET_UNREACHABLE instead
    static final byte DEST_HOST_UNKNOWN = 7;
    // static final byte PROHIBITED_NETWORK = 9;    // By RFC1812, should use PROHIBITED instead
    // static final byte PROHIBITED_HOST = 10;      // By RFC1812, should use PROHIBITED instead
    static final byte PROHIBITED = 13;

    /**
     * May only be called before session is established (from SessionRequest handler)
     */
    void rejectReturnUnreachable( byte code );

    /**
     * Release this session from processing.
     * If called in the SessionRequest handler, the session is entirely released from this node
     * If called later, then the session still flows through this node, but no events are called.
     */
    void release();

    void endpoint();

    InetAddress getNatFromHost();
    int getNatFromPort();
    InetAddress getNatToHost();
    int getNatToPort();

    /**
     * Returns the protocol for the session.</p>
     * @return a <code>short</code> giving one of the protocols (right now always TCP or UDP)
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

}
