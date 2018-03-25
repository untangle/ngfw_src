/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.net.InetAddress;

import com.untangle.uvm.app.SessionEvent;

/**
 * The new IP session request interface
 */
public interface IPNewSessionRequest extends NewSessionRequest
{
    static final short PROTO_TCP = 6;
    static final short PROTO_UDP = 17;
    
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
     * Sets the client address for this session.
     */
    void setNewClientAddr( InetAddress addr );

    /**
     * Sets the client port for this session.
     */
    void setNewClientPort( int port );

    /**
     * Sets the server address for this session.
     */
    void setNewServerAddr( InetAddress addr );

    /**
     * Sets the server port for this session.
     */
    void setNewServerPort( int port );

    /**
     * Get the Session Event for this session
     */
    SessionEvent sessionEvent();

    /**
     * May only be called before session is established (from SessionRequest handler)
     */
    void rejectSilently();
    
    /**
     * May only be called before session is established (from SessionRequest handler)
     */
    void rejectReturnUnreachable( byte code );

    /**
     * Release this session from processing.
     * If called in the SessionRequest handler, the session is entirely released from this app
     * If called later, then the session still flows through this app, but no events are called.
     */
    void release();

    /**
     * Unused? XXX
     */
    void endpoint();

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
     * Gets the original (pre-NAT) Client Address of this session. </p>
     *
     * @return  the client address
     */
    InetAddress getOrigClientAddr();

    /**
     * Gets the new (post-NAT) Client Address of this session. </p>
     *
     * @return  the client address
     */
    InetAddress getNewClientAddr();
    
    /**
     * Gets the original (pre-NAT) Server Address of this session. </p>
     *
     * @return  the server addr.
     */
    InetAddress getOrigServerAddr();

    /**
     * Gets the new (post-NAT) Server Address of this session. </p>
     *
     * @return  the server addr.
     */
    InetAddress getNewServerAddr();
    
    /**
     * Gets the original (pre-NAT) client port for this session.</p>
     * @return the client port.
     */
    int getOrigClientPort();

    /**
     * Gets the new (post-NAT) client port for this session.</p>
     * @return the client port.
     */
    int getNewClientPort();
    
    /**
     * Gets the original (pre-NAT) server port for this session.</p>
     * @return the server port.
     */
    int getOrigServerPort();

    /**
     * Gets the new (post-NAT) server port for this session.</p>
     * @return the server port.
     */
    int getNewServerPort();
}
