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


import com.untangle.mvvm.api.IPSessionDesc;
import com.untangle.mvvm.api.SessionEndpoints;

import com.untangle.mvvm.tran.IPMaddr;
import com.untangle.mvvm.tran.PortRange;
import org.apache.log4j.Logger;

/**
 * A traffic subscription. Right now these are internal, the user does
 * not mess with subscriptions until we have a use case where this
 * makes sense, probably the best option would be for a transform to
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
    private final boolean inbound;
    private final boolean outbound;
    private final IPMaddr serverAddress;
    private final IPMaddr clientAddress;
    private final PortRange serverRange;
    private final PortRange clientRange;

    // constructors -----------------------------------------------------------

    public Subscription(Protocol protocol,
                        boolean inbound, boolean outbound,
                        IPMaddr clientAddress, PortRange clientRange,
                        IPMaddr serverAddress, PortRange serverRange)
    {
        this.protocol = protocol;
        this.inbound = inbound;
        this.outbound = outbound;
        this.clientAddress = clientAddress;
        this.clientRange = clientRange;
        this.serverAddress = serverAddress;
        this.serverRange = serverRange;
    }

    public Subscription(Protocol protocol)
    {
        this.protocol = protocol;
        this.inbound = true;
        this.outbound = true;
        this.serverAddress = IPMaddr.anyAddr;
        this.clientAddress = IPMaddr.anyAddr;
        this.serverRange = PortRange.ANY;
        this.clientRange = PortRange.ANY;
    }

    public Subscription(Protocol protocol, boolean inbound, boolean outbound)
    {
        this.protocol = protocol;
        this.inbound = inbound;
        this.outbound = outbound;
        this.serverAddress = IPMaddr.anyAddr;
        this.clientAddress = IPMaddr.anyAddr;
        this.serverRange = PortRange.ANY;
        this.clientRange = PortRange.ANY;
    }

    // business methods -------------------------------------------------------

    public boolean matches(IPSessionDesc sessionDesc, boolean sessionInbound)
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

        if ((sessionInbound && !inbound) || (!sessionInbound && !outbound)) {
            return false;
        } else if (!clientAddress.contains(sessionDesc.clientAddr())) {
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
     * Whether or not to match for inbound sessions
     *
     * @return true if we should match inbound sessions
     */
    public boolean isInbound()
    {
        return inbound;
    }

    /**
     * Whether or not to match for outbound sessions
     *
     * @return true if we should match outbound sessions
     */
    public boolean isOutbound()
    {
        return outbound;
    }

    /**
     * Server address.
     *
     * @return server address.
     */
    public IPMaddr getServerAddress()
    {
        return serverAddress;
    }

    /**
     * Client address.
     *
     * @return client address.
     */
    public IPMaddr getClientAddress()
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
            && s.inbound == inbound
            && s.outbound == outbound
            && s.clientAddress.equals(clientAddress)
            && s.serverAddress.equals(serverAddress)
            && s.clientRange.equals(clientRange)
            && s.serverRange.equals(serverRange);
    }

    public int hashCode()
    {
        int result = 17;
        result = 37 * result + protocol.hashCode();
        if (inbound)
            result = 23 * result;
        if (outbound)
            result = 27 * result;
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
