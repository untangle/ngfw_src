/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.argon;

import java.net.InetAddress;

import com.untangle.uvm.node.PipelineEndpoints;

public interface ArgonIPNewSessionRequest extends ArgonNewSessionRequest, ArgonIPSessionDesc
{
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

    PipelineEndpoints pipelineEndpoints();

    /**
     * Session requests have a state of <code>REQUESTED</code> when the session request has not
     * been rejected or released by the node.
     */
    static final byte REQUESTED = 2;

    /**
     * Session requests have a state of <code>REJECTED</code> when the session request has
     * been rejected by the node.  It cannot aftwards be released.
     */
    static final byte REJECTED = 99;

    /**
     * Session requests have a state of <code>RELEASED</code> when the session request has been
     * been released by the node.  It cannot aftarwards be rejected.
     */
    static final byte RELEASED = 98;

    /**
     * Session requests have a state of <code>ENDPOINTED</code> when the session request 
     * wants to endpoint the connection.  The current state must be REQUESTED.
     */
    static final byte ENDPOINTED = 100;

    /**
     * Session requests have a state of <code>REJECTED_SILENT</code> when the session request
     * has been rejected silently by the node.  The curren state must be REQUESTED.
     */
    static final byte REJECTED_SILENT = 101;

    // (We need no 'ALLOWED' state as we auto-transition there from REQUESTED after the event
    // has been delivered.

    // One of REQUESTED, REJECTED, RELEASED 
    byte state();

    /**
     * Guardian has requested to endpoint the connection.  You can only endpoint if the session
     * is still in the REQUESTED state.
     */
    public void endpoint();
    
    // May only be called before session is established (from UDPNewSessionRequestEvent handler) 
    void rejectSilently();

    // Codes for rejectReturnUnreachable() and for reset
    static final byte NET_UNREACHABLE = 0;
    static final byte HOST_UNREACHABLE = 1;
    static final byte PROTOCOL_UNREACHABLE = 2;
    static final byte PORT_UNREACHABLE = 3;
    // static final byte DEST_NETWORK_UNKNOWN = 6;  // By RFC1812, should use NET_UNREACHABLE instead
    static final byte DEST_HOST_UNKNOWN = 7;
    // static final byte PROHIBITED_NETWORK = 9;    // By RFC1812, should use PROHIBITED instead
    // static final byte PROHIBITED_HOST = 10;      // By RFC1812, should use PROHIBITED instead
    static final byte PROHIBITED = 13;
    // Only valid for TCP connections
    static final byte TCP_REJECT_RESET = 64;

    // Retrieve the reject code.
    byte rejectCode();

    // May only be called before session is established (from UDPNewSessionRequestEvent handler) 
    void rejectReturnUnreachable( byte code );

    // May only be called before session is established (from TCPNewSessionRequestEvent handler)
    void release();

    public InetAddress getNatFromHost();
    public int getNatFromPort();
    public InetAddress getNatToHost();
    public int getNatToPort();

}
