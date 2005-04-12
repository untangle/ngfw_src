/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tapi;

import java.net.InetAddress;
import com.metavize.mvvm.argon.SessionEndpoints;

public interface IPNewSessionRequest extends NewSessionRequest, SessionEndpoints {

    /**
     * Sets the client address for this session.</p>
     */
    void clientAddr( InetAddress addr );

    /**
     * Sets the client port for this session.</p>
     */
    void clientPort( int port );

    /**
     * Sets the server address for this session.</p>
     */
    void serverAddr( InetAddress addr );

    /**
     * Sets the server port for this session.</p>
     */
    void serverPort( int port );

    /**
     * Sets the server interface.</p>
     */
    void serverIntf( byte intf );

    // May only be called before session is established (from UDPNewSessionRequestEvent handler)
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

    // May only be called before session is established (from UDPNewSessionRequestEvent handler)
    void rejectReturnUnreachable( byte code );

    // May only be called before session is established (from TCPNewSessionRequestEvent handler)
    void release();
    void endpoint();
}
