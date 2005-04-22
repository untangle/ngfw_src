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

import com.metavize.jnetcap.Netcap;
import com.metavize.mvvm.argon.IPSessionDesc;
import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.mvvm.tran.PortRange;
import org.apache.log4j.Logger;

/**
 * A traffic subscription. Right now these are internal, the user does
 * not mess with subscriptions until we have a use case where this
 * makes sense, probably the best option would be for a transform to
 * provide methods that would do the subscription making work for the
 * client.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class Subscription
{
    private static final Logger logger = Logger.getLogger(Subscription.class);

    private final Protocol protocol;
    private final Interface clientInterface;
    private final Interface serverInterface;
    private final IPMaddr serverAddress;
    private final IPMaddr clientAddress;
    private final PortRange serverRange;
    private final PortRange clientRange;

    // constructors -----------------------------------------------------------

    public Subscription(Protocol protocol,
                        Interface clientInterface, Interface serverInterface,
                        IPMaddr clientAddress, PortRange clientRange,
                        IPMaddr serverAddress, PortRange serverRange)
    {
        this.protocol = protocol;
        this.clientInterface = clientInterface;
        this.serverInterface = serverInterface;
        this.clientAddress = clientAddress;
        this.clientRange = clientRange;
        this.serverAddress = serverAddress;
        this.serverRange = serverRange;
    }

    public Subscription(Protocol protocol)
    {
        this.protocol = protocol;

        this.clientInterface = Interface.ANY;
        this.serverInterface = Interface.ANY;
        this.serverAddress = IPMaddr.anyAddr;
        this.clientAddress = IPMaddr.anyAddr;
        this.serverRange = PortRange.ANY;
        this.clientRange = PortRange.ANY;
    }

    public Subscription(Protocol protocol, Interface clientInterface,
                        Interface serverInterface)
    {
        this.protocol = protocol;
        this.clientInterface = clientInterface;
        this.serverInterface = serverInterface;

        this.serverAddress = IPMaddr.anyAddr;
        this.clientAddress = IPMaddr.anyAddr;
        this.serverRange = PortRange.ANY;
        this.clientRange = PortRange.ANY;
    }

    // business methods -------------------------------------------------------

    public boolean matches(IPSessionDesc sessionDesc)
    {
        switch (sessionDesc.protocol()) {
        case Netcap.IPPROTO_TCP:
            if (Protocol.TCP != protocol) { return false; }
            break;

        case Netcap.IPPROTO_UDP:
            if (Protocol.UDP == protocol) { return false; }
            break;

        default:
            logger.warn("unsupported protocol: " + sessionDesc.protocol());
            return false;
        }

        if (!clientInterface.matches(sessionDesc.clientIntf())) {
            return false;
        } else if (!serverInterface.matches(sessionDesc.serverIntf())) {
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
     * Interface of client.
     *
     * @return the client interface.
     */
    public Interface getClientInterface()
    {
        return clientInterface;
    }

    /**
     * Interface of server.
     *
     * @return the server interface.
     */
    public Interface getServerInterface()
    {
        return serverInterface;
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
            && s.clientInterface == clientInterface
            && s.serverInterface == serverInterface
            && s.clientAddress.equals(clientAddress)
            && s.serverAddress.equals(serverAddress)
            && s.clientRange.equals(clientRange)
            && s.serverRange.equals(serverRange);
    }

    public int hashCode()
    {
        int result = 17;
        result = 37 * result + protocol.hashCode();
        result = 37 * result + clientInterface.hashCode();
        result = 37 * result + serverInterface.hashCode();
        result = 37 * result + clientAddress.hashCode();
        result = 37 * result + serverAddress.hashCode();
        result = 37 * result + clientRange.hashCode();
        result = 37 * result + serverRange.hashCode();

        return result;
    }
}
