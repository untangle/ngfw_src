/*
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.net.InetAddress;

import com.untangle.uvm.node.PipelineEndpoints;
import com.untangle.uvm.node.SessionEndpoints;

/**
 * The new IP session request interface
 */
public interface IPNewSessionRequest extends NewSessionRequest, SessionEndpoints
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

    PipelineEndpoints pipelineEndpoints();

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
}
