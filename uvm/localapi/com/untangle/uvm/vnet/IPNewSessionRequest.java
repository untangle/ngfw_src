/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.vnet;

import java.net.InetAddress;

import com.untangle.uvm.node.PipelineEndpoints;
import com.untangle.uvm.node.SessionEndpoints;

/**
 * The new IP session request interface
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
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

    // May only be called before session is established (from
    // UDPNewSessionRequestEvent handler)
    void rejectSilently();

    // May only be called before session is established (from
    // UDPNewSessionRequestEvent handler)
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

    // May only be called before session is established (from
    // UDPNewSessionRequestEvent handler)
    void rejectReturnUnreachable(byte code);

    // May only be called before session is established (from
    // UDPNewSessionRequestEvent handler)
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
