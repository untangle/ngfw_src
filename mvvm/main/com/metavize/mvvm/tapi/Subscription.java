/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Subscription.java,v 1.1 2005/01/30 09:20:31 amread Exp $
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

    private Protocol protocol;
    private Interface clientInterface = Interface.ANY;
    private Interface serverInterface = Interface.ANY;
    private IPMaddr serverAddress = IPMaddr.anyAddr;
    private IPMaddr clientAddress = IPMaddr.anyAddr;
    private PortRange serverRange = PortRange.ANY;
    private PortRange clientRange = PortRange.ANY;

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
    }

    public Subscription(Protocol protocol, Interface clientInterface,
                        Interface serverInterface)
    {
        this.protocol = protocol;
        this.clientInterface = clientInterface;
        this.serverInterface = serverInterface;
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

    public void setProtocol(Protocol protocol)
    {
        this.protocol = protocol;
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

    public void setClientInterface(Interface clientInterface)
    {
        this.clientInterface = clientInterface;
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

    public void setServerInterface(Interface serverInterface)
    {
        this.serverInterface = serverInterface;
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

    public void setServerAddress(IPMaddr serverAddress)
    {
        this.serverAddress = serverAddress;
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

    public void setClientAddress(IPMaddr clientAddress)
    {
        this.clientAddress = clientAddress;
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

    public void setServerRange(PortRange serverRange)
    {
        this.serverRange = serverRange;
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

    public void setClientRange(PortRange clientRange)
    {
        this.clientRange = clientRange;
    }
}
