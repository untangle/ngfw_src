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
     * @return a <code>byte</code> giving the client interface of the session.
     */
    byte clientIntf();

    /**
     * Returns an argon interface for the server.</p>
     *
     * @return a <code>byte</code> giving the server interface of the session.
     */
    byte serverIntf();

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
