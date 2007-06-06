/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.tapi;

import java.net.InetAddress;
import com.untangle.mvvm.tran.PipelineEndpoints;
import com.untangle.mvvm.api.SessionEndpoints;

public interface IPNewSessionRequest extends NewSessionRequest, SessionEndpoints {

    /**
     * Sessions are inbound when the inbound side of the policy is selected.  This is decided
     * at welding time and is no longer dependent only on the client/server interfaces.
     *
     * @return true if the session is inbound, false if it is outbound
     */
    boolean isInbound();

    /**
     * Sessions are outbound when the outbound side of the policy is selected.  This is decided
     * at welding time and is no longer dependent only on the client/server interfaces.  This is
     * the inverse of <code>isInbound</code>
     *
     * @return true if the session is outbound, false if it is inbound
     */
    boolean isOutbound();

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

    /**
     * Get the original server interface before any of the overrides occurred 
     */
    byte originalServerIntf();

    PipelineEndpoints pipelineEndpoints();

    // May only be called before session is established (from UDPNewSessionRequestEvent handler)
    void rejectSilently();

    // May only be called before session is established (from UDPNewSessionRequestEvent handler)
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

    // May only be called before session is established (from UDPNewSessionRequestEvent handler)
    void rejectReturnUnreachable(byte code);

    // May only be called before session is established (from UDPNewSessionRequestEvent handler)
    void rejectReturnUnreachable(byte code, boolean needsFinalization);

    /**
     * <code>release</code> notifies the TAPI that this session may continue with the current settings
     * (which may be modified, IE: NAT modifies the endpoint), but no data events will be delivered for
     * the session.  If needsFinalization is false, no further events will be delivered for the session
     * at all.  IF needsFinalization is true, then the only event that will be delivered is a Finalization
     * event when the resulting session ends.
     *
     * @param needsFinalization a <code>boolean</code> true if the transform needs a finalization event when the released session ends.
     */
    void release(boolean needsFinalization);

    /**
     * This is just release(false);
     *
     */
    void release();

    void endpoint();
}
