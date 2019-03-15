/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import org.apache.log4j.Logger;

import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.app.PortRange;
import com.untangle.uvm.app.SessionTuple;

/**
 * A traffic subscription. Right now these are internal, the user does
 * not mess with subscriptions until we have a use case where this
 * makes sense, probably the best option would be for a app to
 * provide methods that would do the subscription making work for the
 * client.
 */
public class Subscription
{
    private final Logger logger = Logger.getLogger(getClass());

    private final Protocol protocol;
    private final IPMaskedAddress serverAddress;
    private final IPMaskedAddress clientAddress;
    private final PortRange serverRange;
    private final PortRange clientRange;

    /**
     * Subscription constructor
     * @param protocol
     * @param clientAddress
     * @param clientRange
     * @param serverAddress
     * @param serverRange
     */
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

    /**
     * Subscription constructor
     * @param protocol
     */
    public Subscription(Protocol protocol)
    {
        this.protocol = protocol;
        this.serverAddress = IPMaskedAddress.anyAddr;
        this.clientAddress = IPMaskedAddress.anyAddr;
        this.serverRange = PortRange.ANY;
        this.clientRange = PortRange.ANY;
    }

    /**
     * matches - returns true if the subscription matches (includes) the tuple
     * @param tuple
     * @return bool
     */
    public boolean matches( SessionTuple tuple )
    {
        switch (tuple.getProtocol()) {
        case SessionTuple.PROTO_TCP:
            if (Protocol.TCP != protocol) { return false; }
            break;

        case SessionTuple.PROTO_UDP:
            if (Protocol.UDP != protocol) { return false; }
            break;

        default:
            logger.warn("unsupported protocol: " + tuple.getProtocol());
            return false;
        }

        if (!clientAddress.contains(tuple.getClientAddr())) {
            return false;
        } else if (!clientRange.contains(tuple.getClientPort())) {
            return false;
        } else if (!serverAddress.contains(tuple.getServerAddr())) {
            return false;
        } else if (!serverRange.contains(tuple.getServerPort())) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Protocol of subscription, TCP or UDP.

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

    /**
     * equals
     * @param o
     * @return bool
     */
    public boolean equals(Object o)
    {
        Subscription s = (Subscription)o;
        if(s == null){
            return false;
        }
        return s.protocol == protocol
            && s.clientAddress.equals(clientAddress)
            && s.serverAddress.equals(serverAddress)
            && s.clientRange.equals(clientRange)
            && s.serverRange.equals(serverRange);
    }

    /**
     * hashCode
     * @return int
     */
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

    /**
     * toString
     * @return string
     */
    public String toString()
    {
        return protocol.toString();
    }
}
