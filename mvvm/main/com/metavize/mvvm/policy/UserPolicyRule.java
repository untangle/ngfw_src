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

package com.metavize.mvvm.policy;

import java.util.*;

import com.metavize.mvvm.argon.IPSessionDesc;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.firewall.PortMatcher;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;

/**
 * User Policy Rules.  These are the policy rules that are created by the user.  All of
 * these run before any of the System policy rules.
 *
 * @author
 * @version 1.0
 * @hibernate.class
 * table="USER_POLICY_RULE"
 */
public class UserPolicyRule extends PolicyRule
{
    /* settings */
    private ProtocolMatcher protocol;

    private IPMatcher   clientAddr;
    private IPMatcher   serverAddr;

    private PortMatcher clientPort;
    private PortMatcher serverPort;

    // constructors -----------------------------------------------------------

    public UserPolicyRule() { }

    public UserPolicyRule(byte clientIntf, byte serverIntf, Policy policy, boolean inbound,
                          ProtocolMatcher protocol, IPMatcher clientAddr, IPMatcher serverAddr,
                          PortMatcher clientPort, PortMatcher serverPort) {
        super(clientIntf, serverIntf, policy, inbound);
        this.protocol = protocol;
        this.clientAddr = clientAddr;
        this.serverAddr = serverAddr;
        this.clientPort = clientPort;
        this.serverPort = serverPort;
    }

    // PolicyRule methods -----------------------------------------------------

    public boolean matches(IPSessionDesc sd)
    {
        return clientIntf == sd.clientIntf()
            && serverIntf == sd.serverIntf()
            && clientAddr.isMatch(sd.clientAddr())
            && serverAddr.isMatch(sd.serverAddr())
            && clientPort.isMatch(sd.clientPort())
            && serverPort.isMatch(sd.serverPort());
    }

    // accessors --------------------------------------------------------------

    /* Hack that sets the ports to zero for Ping sessions */
    public void fixPing() throws ParseException
    {
        if ( this.protocol.equals( ProtocolMatcher.MATCHER_PING )) {
            this.clientPort = PortMatcher.MATCHER_PING;
            this.serverPort = PortMatcher.MATCHER_PING;
        } else if ( this.clientPort.equals( PortMatcher.MATCHER_PING ) ||
                    this.serverPort.equals( PortMatcher.MATCHER_PING )) {
            throw new ParseException( "Invalid port for a non-ping traffic type" );
        }
    }

    /**
     * Protocol matcher
     *
     * @return the protocol matcher.
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.ProtocolMatcherUserType"
     * @hibernate.column
     * name="PROTOCOL_MATCHER"
     */
    public ProtocolMatcher getProtocol()
    {
        return protocol;
    }

    public void setProtocol( ProtocolMatcher protocol )
    {
        this.protocol = protocol;
    }

    /**
     * client address IPMatcher
     *
     * @return the client address IP matcher.
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.IPMatcherUserType"
     * @hibernate.column
     * name="CLIENT_IP_MATCHER"
     */
    public IPMatcher getClientAddr()
    {
        return clientAddr;
    }

    public void setClientAddr(IPMatcher clientAddr)
    {
        this.clientAddr = clientAddr;
    }

    /**
     * server address IPMatcher
     *
     * @return the server address IP matcher.
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.IPMatcherUserType"
     * @hibernate.column
     * name="SERVER_IP_MATCHER"
     */
    public IPMatcher getServerAddr()
    {
        return serverAddr;
    }

    public void setServerAddr(IPMatcher serverAddr)
    {
        this.serverAddr = serverAddr;
    }

    /**
     * client port PortMatcher
     *
     * @return the client port matcher.
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.PortMatcherUserType"
     * @hibernate.column
     * name="CLIENT_PORT_MATCHER"
     */
    public PortMatcher getClientPort()
    {
        return clientPort;
    }

    public void setClientPort(PortMatcher clientPort)
    {
        this.clientPort = clientPort;
    }

    /**
     * server port PortMatcher
     *
     * @return the server port matcher.
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.PortMatcherUserType"
     * @hibernate.column
     * name="SERVER_PORT_MATCHER"
     */
    public PortMatcher getServerPort()
    {
        return serverPort;
    }

    public void setServerPort(PortMatcher serverPort)
    {
        this.serverPort = serverPort;
    }

    public boolean isSameRow(UserPolicyRule pr)
    {
        return getId().equals(pr.getId());
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof UserPolicyRule)) {
            return false;
        } else {
            UserPolicyRule pr = (UserPolicyRule)o;
            return (policy.equals(pr.policy) &&
                    clientIntf == pr.clientIntf &&
                    serverIntf == pr.serverIntf &&
                    inbound == pr.inbound &&
                    protocol.equals(pr.protocol) &&
                    clientAddr.equals(pr.clientAddr) &&
                    serverAddr.equals(pr.serverAddr) &&
                    clientPort.equals(pr.clientPort) &&
                    serverPort.equals(pr.serverPort));
        }
    }

    public int hashCode()
    {
        // Should be fixed to include other stuff, once those are fixed. XXX
        return (null == policy ? 0 : policy.hashCode()) + clientIntf * 7 + serverIntf * 5;
    }
}
