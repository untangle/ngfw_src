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


import org.apache.log4j.Logger;

import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.node.IPSessionDesc;
import com.untangle.uvm.node.PortRange;
import com.untangle.uvm.node.SessionEndpoints;

/**
 * A traffic subscription. Right now these are internal, the user does
 * not mess with subscriptions until we have a use case where this
 * makes sense, probably the best option would be for a node to
 * provide methods that would do the subscription making work for the
 * client.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class Subscription
{
    private final Logger logger = Logger.getLogger(getClass());

    private final Protocol protocol;
    private final IPMaskedAddress serverAddress;
    private final IPMaskedAddress clientAddress;
    private final PortRange serverRange;
    private final PortRange clientRange;

    // constructors -----------------------------------------------------------

    public Subscription(Protocol protocol,
                        IPMaskedAddress clientAddress, PortRange clientRange,
                        IPMaskedAddress serverAddress, PortRange serverRange)
    {
        this.protocol = protocol;
        this.clientAddress = clientAddress;
        this.clientRange = clientRange;
        this.serverAddress = serverAddress;
        this.serverRange = serverRange;
    }

    public Subscription(Protocol protocol)
    {
        this.protocol = protocol;
        this.serverAddress = IPMaskedAddress.anyAddr;
        this.clientAddress = IPMaskedAddress.anyAddr;
        this.serverRange = PortRange.ANY;
        this.clientRange = PortRange.ANY;
    }

    // business methods -------------------------------------------------------

    public boolean matches(IPSessionDesc sessionDesc)
    {
        switch (sessionDesc.protocol()) {
        case SessionEndpoints.PROTO_TCP:
            if (Protocol.TCP != protocol) { return false; }
            break;

        case SessionEndpoints.PROTO_UDP:
            if (Protocol.UDP != protocol) { return false; }
            break;

        default:
            logger.warn("unsupported protocol: " + sessionDesc.protocol());
            return false;
        }

        if (!clientAddress.contains(sessionDesc.clientAddr())) {
            return false;
        } else if (!clientRange.contains(sessionDesc.clientPort())) {
            return false;
        } else if (!serverAddress.contains(sessionDesc.serverAddr())) {
            return false;
        } else if (!serverRange.contains(sessionDesc.serverPort())) {
            return false;
        } else {
            return true;
        }
    }

    // accessors --------------------------------------------------------------

    /**
     * Protocol of subscription, TCP or UDP.
     *
     * @return the protocol.
     */
    public Protocol getProtocol()
    {
        return protocol;
    }

    /**
     * Server address.
     *
     * @return server address.
     */
    public IPMaskedAddress getServerAddress()
    {
        return serverAddress;
    }

    /**
     * Client address.
     *
     * @return client address.
     */
    public IPMaskedAddress getClientAddress()
    {
        return clientAddress;
    }

    /**
     * Server range.
     *
     * @return server range.
     */
    public PortRange getServerRange()
    {
        return serverRange;
    }

    /**
     * Client range;
     *
     * @return client range.
     */
    public PortRange getClientRange()
    {
        return clientRange;
    }

    // objects ----------------------------------------------------------------

    public boolean equals(Object o)
    {
        Subscription s = (Subscription)o;
        return s.protocol == protocol
            && s.clientAddress.equals(clientAddress)
            && s.serverAddress.equals(serverAddress)
            && s.clientRange.equals(clientRange)
            && s.serverRange.equals(serverRange);
    }

    public int hashCode()
    {
        int result = 17;
        result = 37 * result + protocol.hashCode();
        result = 37 * result + clientAddress.hashCode();
        result = 37 * result + serverAddress.hashCode();
        result = 37 * result + clientRange.hashCode();
        result = 37 * result + serverRange.hashCode();

        return result;
    }

    public String toString()
    {
        return protocol.toString();
    }
}
