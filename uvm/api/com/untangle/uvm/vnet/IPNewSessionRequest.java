/*
 * $Id$
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
    void clientAddr( InetAddress addr );

    /**
     * Sets the client port for this session.
     */
    void clientPort( int port );

    /**
     * Sets the server address for this session.
     */
    void serverAddr( InetAddress addr );

    /**
     * Sets the server port for this session.
     */
    void serverPort( int port );

    /**
     *
     */
    SessionEvent sessionEvent();

    /**
     * May only be called before session is established (from
     * UDPNewSessionRequestEvent handler)
     */
    void rejectSilently();

    /**
     * May only be called before session is established (from
     * UDPNewSessionRequestEvent handler)
     */
    void rejectSilently(boolean needsFinalization);

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
     * May only be called before session is established (from
     * UDPNewSessionRequestEvent handler)
     */
    void rejectReturnUnreachable(byte code);

    /**
     * May only be called before session is established (from
     * UDPNewSessionRequestEvent handler)
     */
    void rejectReturnUnreachable(byte code, boolean needsFinalization);

    /**
     * <code>release</code> notifies the TAPI that this session may
     * continue with the current settings (which may be modified, IE:
     * NAT modifies the endpoint), but no data events will be
     * delivered for the session.  If needsFinalization is false, no
     * further events will be delivered for the session at all.  IF
     * needsFinalization is true, then the only event that will be
     * delivered is a Finalization event when the resulting session
     * ends.
     *
     * @param needsFinalization a <code>boolean</code> true if the
     * node needs a finalization event when the released session ends.
     */
    void release(boolean needsFinalization);

    /**
     * This is just release(false);
     *
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
